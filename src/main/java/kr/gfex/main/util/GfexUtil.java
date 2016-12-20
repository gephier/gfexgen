package kr.gfex.main.util;

import java.awt.Color;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.api.Partition;
import org.gephi.appearance.api.PartitionFunction;
import org.gephi.appearance.plugin.PartitionElementColorTransformer;
import org.gephi.appearance.plugin.palette.Palette;
import org.gephi.appearance.plugin.palette.PaletteManager;
import org.gephi.graph.api.Column;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.EdgeDirectionDefault;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.DependantColor;
import org.gephi.preview.types.DependantOriginalColor;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.	;

public class GfexUtil {

	static String NODE_FILE_NAME = "node.csv";
	static String EDGE_FILE_NAME = "edge.csv";
	static String SPACE4 = "    ";
	static String COMMA = ",";
	static String GML_ROOT_FORMAT_STA = "graph [\n  directed 1";
	static String GML_ROOT_FORMAT_END = "]";
	static String NODE_SHELL = "  node [\n%s  ]\n";
	static String EDGE_SHELL = "  edge [\n%s  ]\n";
	public static String genGml(String path, String fileNm, int kVal) throws IOException {

		path = path.substring(path.length()-1).equals(File.separator)? path : String.format("%s%s", path, File.separator);
		String gmlPath = String.format("%s%s%s", path, fileNm, ".gml");
		try (
			BufferedReader brNode = new BufferedReader(new InputStreamReader(new FileInputStream(new File(String.format(path, NODE_FILE_NAME)))));
			BufferedReader brEdge = new BufferedReader(new InputStreamReader(new FileInputStream(new File(String.format(path, EDGE_FILE_NAME)))));
			PrintWriter pwGml = new PrintWriter(new BufferedWriter(new FileWriter(gmlPath))); ) {

			pwGml.println(GML_ROOT_FORMAT_STA);

			// Node 생성
			String[] ndHead = brNode.readLine().split(COMMA);
			String instr = brNode.readLine();
			while(instr != null) {
				pwGml.println(getGmlItem(NODE_SHELL, ndHead, instr.split(COMMA)));
				instr = brNode.readLine();
			}
			// Cluster Node 생성
			if(kVal >= 1) {
				String[] cls = ndHead.clone();
				for(int c = 0; c < kVal; c++) {
					Arrays.fill(cls, String.valueOf(c));
					pwGml.println(getGmlItem(NODE_SHELL, ndHead, cls));
				}
			}
			// Edge 생성
			String[] egHead = brEdge.readLine().split(COMMA);
			instr = brEdge.readLine();
			while(instr != null) {
				pwGml.println(getGmlItem(EDGE_SHELL, egHead, instr.split(COMMA)));
				instr = brEdge.readLine();
			}

			pwGml.println(GML_ROOT_FORMAT_END);
		}
		return gmlPath;
	}

	static String ITEM = "    %s %s\n";
	private static String getGmlItem(String Shell, String[] header, String[] tokens) {

		StringBuffer node = new StringBuffer();
		for(int i = 0; i < header.length; i++) {
			node.append(String.format(ITEM, header[i], tokens[i]));
		}
		return String.format(Shell, node.toString());
	}


	public static void genPartitionGraph(String gmlPath, String outputPath) {

		//Init a project - and therefore a workspace
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();

		//Get controllers and models
		ImportController importController = Lookup.getDefault().lookup(ImportController.class);
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
		PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
		AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
		AppearanceModel appearanceModel = appearanceController.getModel();

		//Import file
		Container container;
		try {
			File file = new File(gmlPath);
//			File file = new File(getClass().getResource("/org/gephi/toolkit/demos/polblogs.gml").toURI());
			container = importController.importFile(file);
			container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);   //Force DIRECTED
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}

		//Append imported data to GraphAPI
		importController.process(container, new DefaultProcessor(), workspace);

		//See if graph is well imported
		DirectedGraph graph = graphModel.getDirectedGraph();
		System.out.println("Nodes: " + graph.getNodeCount());
		System.out.println("Edges: " + graph.getEdgeCount());

		//Partition with 'source' column, which is in the data
		Column column = graphModel.getNodeTable().getColumn("variants");
		Function func = appearanceModel.getNodeFunction(graph, column, PartitionElementColorTransformer.class);
		Partition partition = ((PartitionFunction) func).getPartition();
		Palette palette = PaletteManager.getInstance().generatePalette(partition.size());
		partition.setColors(palette.getColors());
		Collection list = partition.getSortedValues();
		Object[] obj = list.toArray();
		for(int t = 0; t < obj.length; t++) {
			String hex = String.format("#%02x%02x%02x", palette.getColors()[t].getRed(), palette.getColors()[t].getGreen(), palette.getColors()[t].getBlue()).toUpperCase();
			System.out.printf("%s : %s : %s (r=%d g=%d b=%d)\n", obj[t] == null? "CLS":obj[t].toString(), palette.getColors()[t], hex,palette.getColors()[t].getRed(),palette.getColors()[t].getGreen(),palette.getColors()[t].getBlue());
		}
		appearanceController.transform(func);
		System.out.println(partition.size() + " partitions found");

		ForceAtlas2 fa2Layout = new ForceAtlas2(new ForceAtlas2Builder());
		fa2Layout.setGraphModel(graphModel);
		fa2Layout.resetPropertiesValues();
		fa2Layout.setEdgeWeightInfluence(1.0);
		fa2Layout.setGravity(0.3);
		fa2Layout.setScalingRatio(20.0);
		fa2Layout.setStrongGravityMode(true);
//		fa2Layout.setBarnesHutTheta(1.2);
//		fa2Layout.setJitterTolerance(0.1);
		fa2Layout.initAlgo();
		for (int i = 0; i < 100 && fa2Layout.canAlgo(); i++) {
			fa2Layout.goAlgo();
		}
		fa2Layout.endAlgo();
		//Preview
//		model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
//		model.getProperties().putValue(PreviewProperty.EDGE_RADIUS, new Float(100.0f));
		model.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
		model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, new Float(0.1f));
		model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
		model.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, new Float(0.5f));

		model.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_COLOR, new DependantColor(DependantColor.Mode.PARENT));
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_OPACITY, 100f);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.BLACK));
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, new Font("Arial", Font.PLAIN, 12));
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_MAX_CHAR, 30);
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_COLOR, new DependantColor(Color.RED));
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_OPACITY, 80f);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_SIZE, 25f);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, true);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_SHORTEN, false);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_SHOW_BOX, true);


//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.RED));
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_COLOR, new DependantColor(Color.YELLOW));
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_SHOW_BOX, true);
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_COLOR, new DependantColor(Color.WHITE));
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_OPACITY, 80f);
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));


		//Export
		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		try {
			ec.exportFile(new File("test1.svg"));
			ec.exportFile(new File("test1.gexf"));
//			ec.exportFile(new File("partition1.pdf"));
		} catch (IOException ex) {
			ex.printStackTrace();
			return;
		}
	}
}

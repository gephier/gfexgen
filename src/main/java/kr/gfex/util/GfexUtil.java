package kr.gfex.util;

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
import java.util.List;

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
import org.openide.util.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GfexUtil {
	private static final Logger logger = LoggerFactory.getLogger(GfexUtil.class);

	static final String NODE_FILE_NAME = "node.csv";
	static final String EDGE_FILE_NAME = "edge.csv";
	static final String SPACE4 = "    ";
	static final String COMMA = ",";
	static final String GML_FILE_EXT = "gml";
	static final String GML_ROOT_FORMAT_STA = "graph [\n  directed 1";
	static final String GML_ROOT_FORMAT_END = "]";
	static final String NODE_CLUSTER = "CLUSTER";
	static final String NODE_SHELL = "  node [\n%s  ]";
	static final String EDGE_SHELL = "  edge [\n%s  ]";
	static final String STR_LABEL = "label";
	public static String genGml(String inPath, String outPath, String fileNm, int kVal) throws IOException {

		String gmlPath = String.format("%s%s%s.%s", outPath, File.separator, fileNm, GML_FILE_EXT);
		try (
			BufferedReader brNode = new BufferedReader(new InputStreamReader(new FileInputStream(new File(String.format("%s%s%s", inPath, File.separator, NODE_FILE_NAME)))));
			BufferedReader brEdge = new BufferedReader(new InputStreamReader(new FileInputStream(new File(String.format("%s%s%s", inPath, File.separator, EDGE_FILE_NAME)))));
			PrintWriter pwGml = new PrintWriter(new BufferedWriter(new FileWriter(gmlPath))) ) {

			pwGml.println(GML_ROOT_FORMAT_STA);

			// generate nodes
			boolean reqLabel = true;
			String strNdHead = brNode.readLine().toLowerCase();
			if(strNdHead.indexOf(STR_LABEL) > 0) {
				reqLabel = false;
			}
			String[] ndHead = strNdHead.split(COMMA);
			String instr = brNode.readLine();
			while(instr != null) {
				pwGml.println(getGmlItem(NODE_SHELL, ndHead, instr.split(COMMA), false));
				instr = brNode.readLine();
			}

			// kval > 0 : generate additional nodes for clusters
			if(kVal > 0) {
				String[] cls = ndHead.clone();
				for(int c = 0; c < kVal; c++) {
					Arrays.fill(cls, NODE_CLUSTER);
					cls[0] = String.valueOf(c);
					pwGml.println(getGmlItem(NODE_SHELL, ndHead, cls, reqLabel));
				}
			}

			// generate edges
			String[] egHead = brEdge.readLine().toLowerCase().split(COMMA);
			instr = brEdge.readLine();
			while(instr != null) {
				pwGml.println(getGmlItem(EDGE_SHELL, egHead, instr.split(COMMA), false));
				instr = brEdge.readLine();
			}

			pwGml.println(GML_ROOT_FORMAT_END);
		}
		return gmlPath;
	}


	static final String GML_ITEM = "    %s %s\n";
	private static String getGmlItem(String Shell, String[] header, String[] tokens, boolean addLabel) {

		StringBuffer node = new StringBuffer();
		for(int i = 0; i < header.length; i++) {
			node.append(String.format(GML_ITEM, header[i], tokens[i]));
		}
		if(addLabel) {
			node.append(String.format(GML_ITEM, STR_LABEL, tokens[0]));
		}
		return String.format(Shell, node.toString());
	}



	static final String GEXF_FILE_EXT = "gexf";
	public static void genPartitionGraph(String gmlPath, String outPath, String fileNm, String partnColNm) throws IOException {

		// Init a project - and therefore a workspace
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();

		// Get controllers and models
		ImportController importController = Lookup.getDefault().lookup(ImportController.class);
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
		PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
		AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
		AppearanceModel appearanceModel = appearanceController.getModel();

		// Import file
		Container container = importController.importFile(new File(gmlPath));
		container.getLoader().setEdgeDefault(EdgeDirectionDefault.DIRECTED);   //Force DIRECTED

		// Append imported data to GraphAPI
		importController.process(container, new DefaultProcessor(), workspace);

		// See if graph is well imported
		DirectedGraph graph = graphModel.getDirectedGraph();
		logger.info("Nodes[" + graph.getNodeCount() +"]");
		logger.info("Edges[" + graph.getEdgeCount() +"]");

		// Partition with 'source' column, which is in the data
		Column column = graphModel.getNodeTable().getColumn(partnColNm);
		Function func = appearanceModel.getNodeFunction(graph, column, PartitionElementColorTransformer.class);
		Partition partition = ((PartitionFunction) func).getPartition();
		Palette palette = PaletteManager.getInstance().generatePalette(partition.size());
		partition.setColors(palette.getColors());
		// Generate partition Legend
		genLegendInfo(partition.getSortedValues().toArray(), partition, palette, outPath, fileNm);

		appearanceController.transform(func);
		logger.info(partition.size() + " partitions found");

		ForceAtlas2 fa2Layout = new ForceAtlas2(new ForceAtlas2Builder());
		fa2Layout.setGraphModel(graphModel);
		fa2Layout.resetPropertiesValues();
		fa2Layout.setEdgeWeightInfluence(1.0);
		fa2Layout.setGravity(0.3);
		fa2Layout.setScalingRatio(20.0);
		fa2Layout.setStrongGravityMode(true);
		fa2Layout.initAlgo();
		for (int i = 0; i < 100 && fa2Layout.canAlgo(); i++) {
			fa2Layout.goAlgo();
		}
		fa2Layout.endAlgo();

		// Preview
		model.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
		model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, new Float(0.1f));
		model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
		model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
		model.getProperties().putValue(PreviewProperty.NODE_BORDER_WIDTH, new Float(0.5f));
		model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_COLOR, new DependantOriginalColor(Color.BLACK));
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, new Font("Arial", Font.PLAIN, 8));
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_MAX_CHAR, 30);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_SHORTEN, false);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, true);
		model.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_OPACITY, 80f);
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_SIZE, 25f);
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_SHOW_BOX, true);
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_COLOR, new DependantColor(DependantColor.Mode.PARENT));
//		model.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_OPACITY, 100f);

		// Export
		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		ec.exportFile(new File(String.format("%s%s%s.%s", outPath, File.separator, fileNm, GEXF_FILE_EXT)));
	}

	static String LEG_FILE_EXT = "leg";
	static final String LEGEND_FORMAT = "%s\t%s";
	static final String COLOR_FORMAT_HEX = "#%02x%02x%02x";
	private static void genLegendInfo(Object[] obj, Partition partition, Palette palette, String outPath, String fileNm) throws IOException {

		try(PrintWriter pwLegend = new PrintWriter(new BufferedWriter(new FileWriter(String.format("%s%s%s.%s", outPath, File.separator, fileNm, LEG_FILE_EXT)))) ) {

			for(int t = 0; t < obj.length; t++) {
				Color color = palette.getColors()[t];
				String hex = String.format(COLOR_FORMAT_HEX, color.getRed(), color.getGreen(), color.getBlue()).toUpperCase();
				pwLegend.println(String.format(LEGEND_FORMAT, obj[t].toString(), hex));
				logger.info(String.format("%s[rgb:%s][hex:%s]", (obj[t] == null? NODE_CLUSTER : obj[t].toString()), color, hex));
			}
		}

	}
}

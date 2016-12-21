package kr.gfex.main;

import java.util.Arrays;

import kr.gfex.util.GfexUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GfexMain {

	private static final int ARGS_LEN = 4;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		new GfexMain().run(args);

	}

	private void run(String[] args) {
		// simple validation for arguments (inPath, outPath, fileNm, partnColNm, kVal)
		if (args == null || args.length < ARGS_LEN) {
			logger.error("Please check the arguments.");
			System.exit(-1);
			return;
		}
		try {
			// if kVal entered...
			int kVal = -1;
			if(args.length > ARGS_LEN) {
				kVal = Integer.parseInt(args[ARGS_LEN]);
			}
			logger.info("GfexMain has run.");
			logger.info("arguments are {}", Arrays.toString(args));

			// generate gml file
			String gml = GfexUtil.genGml(args[0], args[1], args[2], kVal);
			logger.info("gmlFile :"+gml);
			// generate gexf file, legend-csv file
			GfexUtil.genPartitionGraph(gml, args[1], args[2], args[3]);
			logger.info("GfexMain has completed.");
			System.exit(0);

		} catch (Exception e) {
			logger.info("GfexMain has failed.");
			System.exit(-1);
		}
	}


}

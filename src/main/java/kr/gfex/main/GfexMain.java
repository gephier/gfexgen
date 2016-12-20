package kr.gfex.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GfexMain {

	private static final int ARGS_LEN = 4;
	private final Logger logger = LoggerFactory.getLogger(getClass());

	public static void main(String[] args) {
		new GfexMain().run(args);

	}

	private void run(String[] args) {

		if (args == null || args.length < ARGS_LEN) {
//			logger.error("Please check the arguments.");
			System.exit(-1);
			return;
		}
//		logger.info("GLIS HDFS Main has run.");
//		logger.info("arguments are {}", Arrays.toString(args));

		try {
//			int coypCnt = GfexUtil.copyToHdfsFile(args[0], Arrays.copyOfRange(args, 1, args.length));
//			logger.info("GLIS HDFS Main has completed. copyCnt = {}", coypCnt);
			System.exit(0);

		} catch (Exception e) {
//			logger.error("GLIS HDFS Main has failed.", e);
			System.exit(-1);
		}
	}


}

package kr.gfex.main;

import org.junit.Test;

public class GfexMainTest {

	@Test
	public void test() {

		// args 갯수 검증  (inPath, outPath, fileNm, partnColNm, kVal)
		GfexMain.main(new String[]{"./src/test/resources/test1/", "./src/test/resources/test1/", "moov", "Variants", "4"});
	}

}

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TranslateTXMLTest {

    @Test
    void TestLanguageSupportedSuccess() {
        assertEquals(true, TranslateTXML.isLanguageSupported("en","fr"));
    }
    @Test
    void TestLanguageSupportedFailure() {
        assertEquals(false, TranslateTXML.isLanguageSupported("enn","fr"));
    }

    @Test
    void TestFileExistSuccess() {
        String s = "source.txml";
        assertEquals(true, TranslateTXML.fileExists(s));
    }

    @Test
    void TestFileExistFailure() {
        String s = "/Users/novicaovuka/eclipse-workspace/txml_translate_2a/";
        assertEquals(s, TranslateTXML.getPath());
    }


    @Test
    void TestGetPathSuccess() {
        String s = "/Users/novicaovuka/eclipse-workspace/txml_translate_2/";
        assertEquals(s, TranslateTXML.getPath());
    }

    @Test
    void TestGetPathFailure() {
        String s = "souerce.txml";
        assertEquals(false, TranslateTXML.fileExists(s));
    }



    @Test
    void TestDuplicateFlagsSuccess() {
        String[] args = new String[]{"-f", "-o", "-s", "t", "-e", "-r","-q","-a"};
        assertEquals(false, TranslateTXML.parseAndCheckFlags(args));

    }

    @Test
    void TestDuplicateFlagsFailure() {
        String[] args = new String[]{"-f", "-o", "-s", "t", "-f", "-o","-s","-t"};
        assertEquals(false, TranslateTXML.duplicateFLags(args));
    }

    @Test
    void testStartsWithFlagSuccess() {
        String[] args = new String[]{"-s","ahah","-o","hehe","-e"};
        assertEquals(true, TranslateTXML.validFlagStartsWithFLag(args));
    }
    @Test
    void testStartsWithFlagFailure() {
        String[] args = new String[]{"-w","-p","-s","-i"};
        assertEquals(false, TranslateTXML.validFlagStartsWithFLag(args));
    }

    @Test
    void testIsValidSuccess() {

        assertEquals(true, TranslateTXML.isAValidFlag("-f"));
    }
    @Test
    void testIsValidFailure() {
        assertEquals(false, TranslateTXML.isAValidFlag("-r"));
    }


    @Test
    void testIsEmptyFlagsSuccess() {
        String[] args = new String[]{};
        assertEquals(true, TranslateTXML.isEmptyArgumentList(args));
    }
    @Test
    void testIsEmptyFlagsFailure() {
        String[] args = new String[]{"random"};
        assertEquals(false, TranslateTXML.isEmptyArgumentList(args));
    }
    @Test
    void testMinimumArgumentSuccess() {
        String[] args = new String[]{"-f", "source.txml", "-o", "fun.txml", "-s", "en", "examp", "test"};
        assertEquals(true, TranslateTXML.isMinimumArgumentNumber(args));

    }

    @Test
    void testMinimumArgumentFailure() {
        String[] args = new String[]{"-f", "source.txml", "-o", "fun.txml", "-s", "en"};
        assertEquals(false, TranslateTXML.isMinimumArgumentNumber(args));
    }


    @Test
    void testExtentionOnRightPlaceSuccess(){
        assertEquals(true, TranslateTXML.endWithTxmlExtention("source.txml", "target.txml"));
    }


    @Test
    void testExtentionOnRightPlaceFailure(){
        assertEquals(false, TranslateTXML.endWithTxmlExtention("source.txml", "targe.txmlta."));
    }
}
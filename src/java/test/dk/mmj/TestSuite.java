package dk.mmj;

import dk.mmj.fhe.TestLWE;
import dk.mmj.matrix.TestLWEUtils;
import dk.mmj.matrix.TestMatrix;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        //FHE
        TestLWE.class,
        TestLWEUtils.class,

        //Matrix
        TestMatrix.class
})
public class TestSuite {
}

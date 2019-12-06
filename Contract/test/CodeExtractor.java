import org.aion.avm.core.dappreading.UserlibJarBuilder;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.tooling.abi.ABICompiler;
import org.aion.avm.tooling.deploy.OptimizedJarBuilder;
import org.aion.avm.utilities.JarBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CodeExtractor {

    @Rule
    public AvmRule avmRule = new AvmRule(false);
    private Class[] otherClasses = {BettingEvents.class, BettingStorage.class};

    @Test
    public void writeToFile() {
        byte[] dappBytes = avmRule.getDappBytes(BettingContract.class, null, 1, otherClasses);
        Helpers.writeBytesToFile(dappBytes, "Contract/test/contract");
    }

    @Test
    public void writeAbi(){
        byte[] dappBytes =  UserlibJarBuilder.buildJarForMainAndClasses(BettingContract.class, otherClasses);
        ABICompiler compiler = ABICompiler.compileJarBytes(dappBytes, 1);
        compiler.writeAbi(System.out, 1);
    }

    @Test
    public void buildJar() {
        byte[] jar = UserlibJarBuilder.buildJarForMainAndClasses(BettingContract.class, otherClasses);
        byte[] optimizedJar = (new OptimizedJarBuilder(false, jar, 1))
                .withUnreachableMethodRemover()
                .withRenamer()
                .withConstantRemover()
                .getOptimizedBytes();

        DataOutputStream dout = null;
        try {
            dout = new DataOutputStream(new FileOutputStream("bettingContract.jar"));
            dout.write(optimizedJar);
            dout.close();
        } catch (IOException e) {
            System.err.println("Failed to create the jar.");
            e.printStackTrace();
        }
    }
}

import org.aion.avm.core.dappreading.UserlibJarBuilder;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.embed.AvmRule;
import org.aion.avm.tooling.abi.ABICompiler;
import org.junit.Rule;
import org.junit.Test;

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
}

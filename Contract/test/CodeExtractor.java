import org.aion.avm.core.util.Helpers;
import org.aion.avm.embed.AvmRule;
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
}

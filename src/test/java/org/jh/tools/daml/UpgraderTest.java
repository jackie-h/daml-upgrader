package org.jh.tools.daml;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UpgraderTest
{

    private void cleanBuildDar(String directory) throws IOException
    {
        File dir = Paths.get(directory).toFile();
        execCmd("daml clean",  dir);
        execCmd("daml build --log-level ERROR",  dir);
    }

    @Test
    public void testEndToEnd() throws IOException
    {
        cleanBuildDar("daml-examples/init/parties");
        cleanBuildDar("daml-examples/scenario1/v1");
        cleanBuildDar("daml-examples/scenario1/v2");

        Process sandbox = startSandbox();

        execCmd("daml script --dar daml-examples/init/parties/.daml/dist/init-1.0.0.dar --script-name Parties:allocateParties --ledger-host localhost --ledger-port 6865");

        String parties = execCmd("daml ledger list-parties --host localhost --port 6865");

        Assert.assertTrue(parties.contains("\"bob\""));

        execCmd("daml ledger upload-dar daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar --host localhost --port 6865");


        //String res2 = execCmd("daml test --files daml-examples/init/parties/daml/Parties.daml");
        //String res = execCmd("daml test --files src/test/resources/Scenario1Test.daml");

        sandbox.destroyForcibly();
    }

    public Process startSandbox() throws IOException
    {
        Process proc = Runtime.getRuntime().exec("daml sandbox");

        try
        {
            TimeUnit.MILLISECONDS.sleep(10);
        }
        catch (InterruptedException e)
        {
            //ok
        }

        return proc;
    }

    public static String execCmd(String cmd) throws IOException
    {
        return execCmd(cmd, null);
    }

    public static String execCmd(String cmd, File dir) throws IOException
    {
        String result = null;
        Process proc = (dir == null) ? Runtime.getRuntime().exec(cmd): Runtime.getRuntime().exec(cmd, new String[]{}, dir);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        // Read the output from the command
        String s = null;
        StringBuilder out = new StringBuilder();
        while ((s = stdInput.readLine()) != null) {
            out.append(s).append("\n");
        }

        // Read any errors from the attempted command
        StringBuilder err = new StringBuilder();
        String e = null;
        while ((e = stdError.readLine()) != null) {
            err.append(e).append("\n");
        }

        Assert.assertEquals("", err.toString());

        return out.toString();
    }

    @Test
    public void testScenario1()
    {
        List<String> result = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v2/.daml/dist/carbon-2.0.0.dar",
                "target");

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains("CarbonCert"));
        Assert.assertTrue(result.contains("CarbonCertProposal"));
    }

    @Test
    public void testSameDarFileProducesNoChange()
    {
        List<String> result = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "target");

        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testDarFilesWithSameContentsAreSame()
    {
        String darPath1 = "daml-examples/scenario2/v1/.daml/dist/carbon-1.0.0.dar";
        String darPath2 = "daml-examples/scenario2/v2/.daml/dist/carbon-1.0.0.dar";
        Dar dar1 = Dar.readDar(darPath1);
        Dar dar2 = Dar.readDar(darPath2);
        Assert.assertEquals("Files with same contents should have the same hash",
                dar1.getDamlLf().getHash(), dar2.getDamlLf().getHash());

        List<String> result = Upgrader.createUpgrades(darPath1, darPath2, "target");

        Assert.assertEquals(0, result.size());
    }
}

package org.jh.tools.daml;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

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
        //cleanBuildDar("daml-examples/init/parties");
        cleanBuildDar("daml-examples/scenario1/v1");
        cleanBuildDar("daml-examples/scenario1/v2");
        cleanBuildDar("daml-examples/init/carbon");
        cleanBuildDar("daml-examples/sample-upgrade/scenario1");

        Process sandbox = startSandbox();

        try
        {

            String partiesStart = execCmd("daml ledger list-parties --host localhost --port 6865");
            Assert.assertTrue(partiesStart.startsWith("Listing parties at localhost:6865"));
            Assert.assertEquals("Only one party", partiesStart.indexOf("PartyDetail"), partiesStart.lastIndexOf("PartyDetail"));

            //It is somewhat unwieldy to set-up parties first and get them back later - https://blog.digitalasset.com/developers/parties-users-daml-2
            //execCmd("daml script --dar daml-examples/init/parties/.daml/dist/init-1.0.0.dar --script-name Parties:allocateParties --ledger-host localhost --ledger-port 6865");

            execCmd("daml ledger upload-dar daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar --host localhost --port 6865");

            //Alice creates a CarbonCertProposal for a CarbonCert with issuer Alice and owner Bob
            //Bob accepts the proposal
            execCmd("daml script --dar daml-examples/init/carbon/.daml/dist/test-contracts-1.0.0.dar --script-name TestContracts:createContracts --ledger-host localhost --ledger-port 6865 --output-file=target/parties.json");

            //Query contracts V1
            execCmd("daml script --dar daml-examples/init/carbon/.daml/dist/test-contracts-1.0.0.dar --script-name TestContracts:queryContracts --ledger-host localhost --ledger-port 6865 --input-file=target/parties.json --output-file=target/contracts.json");

            String parties = execCmd("daml ledger list-parties --host localhost --port 6865");

            Assert.assertTrue(parties.contains("\"bob\""));

            //Upload the upgrade code
            execCmd("daml ledger upload-dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --host localhost --port 6865");

            //Create the trigger for Bob
            CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
                try
                {
                    return execCmd("daml trigger --dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --trigger-name UpgradeTrigger:upgradeTrigger --ledger-host localhost --ledger-port 6865 --ledger-party=bob");
                }
                catch (IOException e)
                {
                    fail();
                    return "Failed";
                }
            });


            //Alice initiaties an upgrade
            execCmd("daml script --dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --script-name InitiateUpgrade:initiateUpgrade --ledger-host localhost --ledger-port 6865 --input-file=target/parties.json");

            //Query for contracts
            execCmd("daml script --dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --script-name InitiateUpgrade:queryUpgrade --ledger-host localhost --ledger-port 6865 --input-file=target/parties.json --output-file=target/contracts.json");
            String outUpgrades = readContractsFile();

            //Wait for trigger to do it's stuff
            try
            {
                TimeUnit.MILLISECONDS.sleep(5000);
            }
            catch (InterruptedException e)
            {
                //ok
            }

            //Query for V1 contracts again
            execCmd("daml script --dar daml-examples/init/carbon/.daml/dist/test-contracts-1.0.0.dar --script-name TestContracts:queryContracts --ledger-host localhost --ledger-port 6865 --input-file=target/parties.json --output-file=target/contracts.json");
            String outCarbonNow = readContractsFile();

            completableFuture.cancel(true);
        }
        finally
        {
            try
            {
                sandbox.descendants().forEach(ProcessHandle::destroy);
                sandbox.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

    }

    private String readContractsFile() throws IOException
    {
        Path pathContracts = Paths.get("target", "contracts.json");
        byte[] encoded = Files.readAllBytes(pathContracts);
        String content = new String(encoded, StandardCharsets.UTF_8);
        System.out.println(content);
        return content;
    }

    public Process startSandbox() throws IOException
    {
        Process proc = Runtime.getRuntime().exec("daml sandbox");



        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

//        try
//        {
//            TimeUnit.MILLISECONDS.sleep(1000);
//        }
//        catch (InterruptedException e)
//        {
//            //ok
//        }

        long end=System.currentTimeMillis() + 15000;

        BufferedReader stdInput = null;
        StringBuilder out = new StringBuilder();
        try {
            stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            while ((System.currentTimeMillis() < end)) {
                if (stdInput.ready()) {
                    String line = stdInput.readLine();
                    out.append(line);
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            try {
//                if (stdInput != null) {
//                    stdInput.close();
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        //String i = (stdInput.ready()) ? stdInput.readLine() : null;

        // Read any errors from the attempted command
        String e = (stdError.ready()) ? stdError.readLine() : null;

        if(e != null)
        {
            fail(e);
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
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        boolean exited = false;
        try
        {
            exited = proc.waitFor(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        


        //proc.getOutputStream()

        // Read any errors from the attempted command
        StringBuilder err = new StringBuilder();
        String e = null;
        while (stdError.ready() && (e = stdError.readLine()) != null) {
            err.append(e).append("\n");
        }
        Assert.assertEquals("", err.toString());

        // Read the output from the command
        String s = null;
        StringBuilder out = new StringBuilder();
        while ((s = stdInput.readLine()) != null) {
            out.append(s).append("\n");
        }

        return out.toString();
    }

    @Test
    public void testScenario1()
    {
        Map<String, List<String>> modulesResult = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v2/.daml/dist/carbon-2.0.0.dar",
                "target");

        List<String> result = modulesResult.get("Carbon");

        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains("CarbonCert"));
        Assert.assertTrue(result.contains("CarbonCertProposal"));
    }

    @Test
    public void testSameDarFileProducesNoChange()
    {
        Map<String, List<String>> result = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
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

        Map<String, List<String>> result = Upgrader.createUpgrades(darPath1, darPath2, "target");

        Assert.assertEquals(0, result.size());
    }
}

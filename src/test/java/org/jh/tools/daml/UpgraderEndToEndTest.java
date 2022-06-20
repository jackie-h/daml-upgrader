package org.jh.tools.daml;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class UpgraderEndToEndTest
{


    @Test
    public void testEndToEnd() throws IOException
    {
        //cleanBuildDar("daml-examples/init/parties");
        DamlCommand.cleanBuildDar("daml-examples/scenario1/v1");
        DamlCommand.cleanBuildDar("daml-examples/scenario1/v2");
        DamlCommand.cleanBuildDar("daml-examples/init/carbon");
        DamlCommand.cleanBuildDar("daml-examples/sample-upgrade/scenario1");

        Process sandbox = startSandbox();

        try
        {

            String partiesStart = DamlCommand.exec("daml ledger list-parties --host localhost --port 6865");
            Assert.assertTrue(partiesStart.startsWith("Listing parties at localhost:6865"));
            Assert.assertEquals("Only one party", partiesStart.indexOf("PartyDetail"), partiesStart.lastIndexOf("PartyDetail"));

            //It is somewhat unwieldy to set-up parties first and get them back later - https://blog.digitalasset.com/developers/parties-users-daml-2
            //DamlCommand.execCmd("daml script --dar daml-examples/init/parties/.daml/dist/init-1.0.0.dar --script-name Parties:allocateParties --ledger-host localhost --ledger-port 6865");

            DamlCommand.exec("daml ledger upload-dar daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar --host localhost --port 6865");

            //Alice creates a CarbonCertProposal for a CarbonCert with issuer Alice and owner Bob
            //Bob accepts the proposal
            DamlCommand.exec("daml script --dar daml-examples/init/carbon/.daml/dist/test-contracts-1.0.0.dar --script-name TestContracts:createContracts --ledger-host localhost --ledger-port 6865 --output-file=target/parties.json");


            Path pathContracts = Paths.get("target", "parties.json");
            byte[] encoded = Files.readAllBytes(pathContracts);
            String content = new String(encoded, StandardCharsets.UTF_8);
            content = content.replace("[", "");
            content = content.replace("]", "");
            String[] partiesSplit = content.split(", ");
            Path pathAlice = Paths.get("target", "alice.json");
            Path pathBob = Paths.get("target", "bob.json");
            String aliceId = partiesSplit[0].replace("\"", "");
            Files.writeString(pathAlice, partiesSplit[0]);
            Files.writeString(pathBob, partiesSplit[1]);

            DamlLedgerQuery client = DamlLedgerQuery.createDamlLedgerQuery("localhost", 6865);
            int active = client.queryActiveContracts(aliceId);

            //Query contracts V1
            DamlCommand.exec("daml script --dar daml-examples/init/carbon/.daml/dist/test-contracts-1.0.0.dar --script-name TestContracts:queryContracts --ledger-host localhost --ledger-port 6865 --input-file=target/alice.json --output-file=target/contracts.json");
            String outCarbonFirst = readContractsFile();

            String parties = DamlCommand.exec("daml ledger list-parties --host localhost --port 6865");

            Assert.assertTrue(parties.contains("\"bob\""));

            //Upload the upgrade code
            DamlCommand.exec("daml ledger upload-dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --host localhost --port 6865");

            //Create the trigger for Bob
            CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
                try
                {
                    return DamlCommand.exec("daml trigger --dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --trigger-name UpgradeTrigger:upgradeTrigger --ledger-host localhost --ledger-port 6865 --ledger-user=alice");
                }
                catch (IOException e)
                {
                    fail();
                    return "Failed";
                }
            });


            //Alice initiaties an upgrade by creating proposals
            DamlCommand.exec("daml script --dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --script-name UpgradeCarbonInitiate:initiateUpgrade --ledger-host localhost --ledger-port 6865 --input-file=target/alice.json");

            //Query for contracts
            DamlCommand.exec("daml script --dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --script-name UpgradeCarbonInitiate:queryUpgrade --ledger-host localhost --ledger-port 6865 --input-file=target/alice.json --output-file=target/contracts.json");
            String outUpgrades = readContractsFile();

            //Bob accepts the proposal
            DamlCommand.exec("daml script --dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --script-name AcceptUpgradeProposal:acceptUpgrade --ledger-host localhost --ledger-port 6865 --input-file=target/bob.json");
            //Query for agreements
            DamlCommand.exec("daml script --dar daml-examples/sample-upgrade/scenario1/.daml/dist/upgrade-1.0.0.dar --script-name AcceptUpgradeProposal:queryUpgradeAgreement --ledger-host localhost --ledger-port 6865 --input-file=target/alice.json --output-file=target/contracts.json");
            String agreements = readContractsFile();

            //Wait for trigger to do it's stuff
            //Trigger should do the upgrade for Bob
            try
            {
                TimeUnit.MILLISECONDS.sleep(5000);
            }
            catch (InterruptedException e)
            {
                //ok
            }

            //Query for V1 contracts again
            DamlCommand.exec("daml script --dar daml-examples/init/carbon/.daml/dist/test-contracts-1.0.0.dar --script-name TestContracts:queryContracts --ledger-host localhost --ledger-port 6865 --input-file=target/alice.json --output-file=target/contracts.json");
            String outCarbonNow = readContractsFile();
            Assert.assertEquals("No V1 templates found", "[]\n", outCarbonNow);

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

    
}

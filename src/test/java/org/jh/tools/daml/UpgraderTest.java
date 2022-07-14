package org.jh.tools.daml;

import com.daml.lf.archive.ArchivePayload;
import com.daml.lf.archive.Reader;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class UpgraderTest
{
    @BeforeClass
    public static void compileDaml() throws IOException
    {
        DamlCommand.cleanBuildDar("daml-examples/data/v1");
        DamlCommand.cleanBuildDar("daml-examples/scenario1/v1");
        DamlCommand.cleanBuildDar("daml-examples/scenario1/v2");
        DamlCommand.cleanBuildDar("daml-examples/scenario2/v1");
        DamlCommand.cleanBuildDar("daml-examples/scenario2/v2");
    }

    @Test
    public void testScenario1() throws IOException
    {
        ArchiveDiffs archiveDiffs = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v2/.daml/dist/carbon-2.0.0.dar",
                "target/scenario1", "../../daml-examples/data/v1/.daml/dist/finance-1.0.0.dar");

        Assert.assertEquals("---------------------------------------------------------------------------------------------------------------\n" +
                        "| Module               | Template              | Result                                                       |\n" +
                        "---------------------------------------------------------------------------------------------------------------\n" +
                        "| Intro.SchemaWithData | ContractWithDataDep   | Template has a type that is currently not supported          |\n" +
                        "| Intro.SchemaWithData | ContractWithDataList  | Template has a type that is currently not supported          |\n" +
                        "| Intro.SchemaWithData | ContractWithData      | Ok!                                                          |\n" +
                        "| Intro.MultiParty     | Agreement             | Don't know how to upgrade contracts with >2 parties yet      |\n" +
                        "| Intro.MultiParty     | Pending               | Don't know how to upgrade contracts with >2 parties yet      |\n" +
                        "| Intro.SchemaChanges  | FieldBecomesMandatory | Template schema changed in a way that is not auto-upgradable |\n" +
                        "| Intro.SchemaChanges  | Same                  | Ok!                                                          |\n" +
                        "| Intro.SchemaChanges  | AddField              | Template schema changed in a way that is not auto-upgradable |\n" +
                        "| Intro.SchemaChanges  | FieldTypeChange       | Template schema changed in a way that is not auto-upgradable |\n" +
                        "| Intro.SchemaChanges  | AddOptionalField      | Ok!                                                          |\n" +
                        "| Intro.SchemaChanges  | RemoveField           | Ok!                                                          |\n" +
                        "| Intro.SchemaChanges  | FieldBecomesOptional  | Template schema changed in a way that is not auto-upgradable |\n" +
                        "| Intro.SchemaChanges  | FieldNameChange       | Template schema changed in a way that is not auto-upgradable |\n" +
                        "| Intro.SchemaChanges  | ReorderField          | Ok!                                                          |\n" +
                        "| Carbon               | CarbonCertProposal    | Ok!                                                          |\n" +
                        "| Carbon               | CarbonCert            | Ok!                                                          |\n" +
                        "| Intro.Invite         | Invitation            | Ok!                                                          |\n" +
                        "| Intro.Iou            | Iou                   | Ok!                                                          |\n" +
                        "---------------------------------------------------------------------------------------------------------------\n",
                archiveDiffs.report());

        DamlCommand.cleanBuildDar("target/scenario1");

        Dar output = Dar.readDar("target/scenario1/.daml/dist/upgrade-1.0.0.dar");
        ArchivePayload result = Reader.readArchive(output.getDamlLf()).right().get();
        List<String> templates = DamlLfProtoUtils.collectTemplateNames(result.proto());

        Assert.assertEquals(17, templates.size());
        Assert.assertEquals("Carbon.UpgradeCarbonCertProposal[UpgradeCarbonCertProposalAgreement]\n" +
                        "Carbon.UpgradeCarbonCert[UpgradeCarbonCertAgreement]\n" +
                        "Carbon.UpgradeCarbonCert[UpgradeCarbonCertProposal]\n" +
                        "Intro.Invite.UpgradeInvitation[UpgradeInvitationAgreement]\n" +
                        "Intro.Invite.UpgradeInvitation[UpgradeInvitationProposal]\n" +
                        "Intro.Iou.UpgradeIou[UpgradeIouAgreement]\n" +
                        "Intro.Iou.UpgradeIou[UpgradeIouProposal]\n" +
                        "Intro.SchemaChanges.UpgradeAddOptionalField[UpgradeAddOptionalFieldAgreement]\n" +
                        "Intro.SchemaChanges.UpgradeAddOptionalField[UpgradeAddOptionalFieldProposal]\n" +
                        "Intro.SchemaChanges.UpgradeRemoveField[UpgradeRemoveFieldAgreement]\n" +
                        "Intro.SchemaChanges.UpgradeRemoveField[UpgradeRemoveFieldProposal]\n" +
                        "Intro.SchemaChanges.UpgradeReorderField[UpgradeReorderFieldAgreement]\n" +
                        "Intro.SchemaChanges.UpgradeReorderField[UpgradeReorderFieldProposal]\n" +
                        "Intro.SchemaChanges.UpgradeSame[UpgradeSameAgreement]\n" +
                        "Intro.SchemaChanges.UpgradeSame[UpgradeSameProposal]\n" +
                        "Intro.SchemaWithData.UpgradeContractWithData[UpgradeContractWithDataAgreement]\n" +
                        "Intro.SchemaWithData.UpgradeContractWithData[UpgradeContractWithDataProposal]",
                String.join("\n", templates));
    }

    @Test
    public void testSameDarFileProducesNoChange()
    {
        ArchiveDiffs result = Upgrader.createUpgrades("daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "daml-examples/scenario1/v1/.daml/dist/carbon-1.0.0.dar",
                "target", null);

        Assert.assertEquals(0, result.templateCount());
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

        ArchiveDiffs result = Upgrader.createUpgrades(darPath1, darPath2, "target", null);

        Assert.assertEquals(0, result.templateCount());
    }
}

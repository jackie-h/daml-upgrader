package org.jh.tools.daml;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class UpgradeTemplate
{
    private static final Logger LOGGER =  Logger.getLogger(UpgradeTemplate.class.getName());

    private static final String CREATE_CONTRACT_TEMPLATE_PART = "         archive certId\n" +
            "         create <module_name>V2.<contract_name> with\n" +
            "<fields:{ field |           <field> = cert.<field>\n }>" +
            "<new_optional_fields:{ field |           <field> = None\n }>";

    private static final String UPGRADE_TEMPLATE_UNILATERAL = "module <module_name>.Upgrade<contract_name> where\n" +
            "\n" +
            "import qualified V1.<module_name> as <module_name>V1\n" +
            "import qualified V2.<module_name> as <module_name>V2\n" +
            "\n" +
            "template Upgrade<contract_name>Agreement\n" +
            "  with\n" +
            "    <sig_issuer> : Party\n" +
            "  where\n" +
            "    signatory <sig_issuer>\n" +
            "    nonconsuming choice Upgrade : ContractId <module_name>V2.<contract_name>\n" +
            "      with\n" +
            "        certId : ContractId <module_name>V1.<contract_name>\n" +
            "      controller <sig_issuer>\n" +
            "      do cert \\<- fetch certId\n" +
            "         assert (cert.<sig_issuer> == <sig_issuer>)\n" +
            CREATE_CONTRACT_TEMPLATE_PART;

    private static final String UPGRADE_TEMPLATE_BILATERAL = "module <module_name>.Upgrade<contract_name> where\n" +
            "\n" +
            "import qualified V1.<module_name> as <module_name>V1\n" +
            "import qualified V2.<module_name> as <module_name>V2\n" +
            "\n" +
            "template Upgrade<contract_name>Proposal\n" +
            "  with\n" +
            "    <sig_issuer> : Party\n" +
            "    <sig_owner> : Party\n" +
            "  where\n" +
            "    signatory <sig_issuer>\n" +
            "    observer <sig_owner>\n" +
            "    key (<sig_issuer>, <sig_owner>) : (Party, Party)\n" +
            "    maintainer key._1\n" +
            "    choice Accept : ContractId Upgrade<contract_name>Agreement\n" +
            "      controller <sig_owner>\n" +
            "      do create Upgrade<contract_name>Agreement with ..\n" +
            "\n" +
            "template Upgrade<contract_name>Agreement\n" +
            "  with\n" +
            "    <sig_issuer> : Party\n" +
            "    <sig_owner> : Party\n" +
            "  where\n" +
            "    signatory <sig_issuer>, <sig_owner>\n" +
            "    key (<sig_issuer>, <sig_owner>) : (Party, Party)\n" +
            "    maintainer key._1\n" +
            "    nonconsuming choice Upgrade : ContractId <module_name>V2.<contract_name>\n" +
            "      with\n" +
            "        certId : ContractId <module_name>V1.<contract_name>\n" +
            "      controller <sig_issuer>\n" +
            "      do cert \\<- fetch certId\n" +
            "         assert (cert.<sig_issuer> == <sig_issuer>)\n" +
            "         assert (cert.<sig_owner> == <sig_owner>)\n" +
            CREATE_CONTRACT_TEMPLATE_PART;

    private static final String UPGRADE_INITIATE_SCRIPT = "module <module_name>.Upgrade<contract_name>Initiate where\n" +
            "\n" +
            "import Daml.Script\n" +
            "import DA.Foldable (forA_)\n" +
            "import DA.List (dedup)\n" +
            "import <module_name>.Upgrade<contract_name>\n" +
            "import qualified V1.<module_name> as <module_name>V1\n" +
            "\n" +
            "\n" +
            "initiateUpgrade : Party -> Script [Party]\n" +
            "initiateUpgrade theOwner = do\n" +
            "  certs \\<- query @<module_name>V1.<contract_name> theOwner\n" +
            "  let myCerts = filter (\\(_cid, c) -> c.<sig_issuer> == theOwner) certs\n" +
            "  let owners = dedup $ map (\\(_cid, c) -> c.<sig_owner>) myCerts\n" +
            "  forA_ owners $ \\owner -> do\n" +
            "    debugRaw (\"Creating upgrade proposal for: \" \\<> show owner)\n" +
            "    submit theOwner $ createCmd (Upgrade<contract_name>Proposal theOwner owner)\n" +
            "  pure( owners )";

    private static final String UPGRADE_PROJECT_YAML = "sdk-version: <sdk_version>\n" +
            "name: upgrade\n" +
            "source: daml\n" +
            "version: 1.0.0\n" +
            "dependencies:\n" +
            "  - daml-prim\n" +
            "  - daml-stdlib\n" +
            "  - daml-script\n" +
            "  - daml-trigger\n" +
            "  - <archive_dep_v1>\n" +
            "  - <archive_dep_v2>\n" +
            "\n" +
            "module-prefixes:\n" +
            "  <archive_name_v1>: V1\n" +
            "  <archive_name_v2>: V2\n";

    public static List<Module> createUpgradeTemplatesContent(String moduleName, List<TemplateDetails> templates,
                                                             ArchiveDiffs archiveDiffs)
    {
        List<Module> contracts = new ArrayList<>();

        templates.stream().filter(TemplateDetails::canAutoUpgrade).forEach(templateDetails ->
        {
            String upgradeModuleName = "Upgrade" + templateDetails.name();
            if (templateDetails.isUnilateral())
            {
                String upgradeTemplate = createUnilateralUpgradeTemplate(moduleName, templateDetails, archiveDiffs);
                contracts.add(new Module(upgradeModuleName, upgradeTemplate));
            }
            else if (templateDetails.isBilateral())
            {
                String upgradeTemplate = createBilateralUpgradeTemplate(moduleName, templateDetails, archiveDiffs);
                contracts.add(new Module(upgradeModuleName, upgradeTemplate));
                String upgradeInitiateScript = createInitiateUpgradeScript(moduleName, templateDetails);
                contracts.add(new Module("Upgrade" + templateDetails.name() + "Initiate", upgradeInitiateScript));
            }
            else
            {
                throw new RuntimeException("Don't know how to upgrade. Template should have been filtered out");
            }
        });
        return contracts;
    }

    private static String createUnilateralUpgradeTemplate(String moduleName, TemplateDetails templateDetails, ArchiveDiffs archiveDiffs)
    {
        ST upgrade = new ST(UPGRADE_TEMPLATE_UNILATERAL);
        populate(upgrade, moduleName, templateDetails.name(), archiveDiffs);
        String issuer = templateDetails.getSignatories().get(0);
        upgrade.add("sig_issuer", issuer);
        return upgrade.render();
    }

    private static String createBilateralUpgradeTemplate(String moduleName, TemplateDetails templateDetails, ArchiveDiffs archiveDiffs)
    {
        ST upgrade = new ST(UPGRADE_TEMPLATE_BILATERAL);
        populate(upgrade, moduleName, templateDetails.name(), archiveDiffs);
        //todo - try to identify the actual owner
        String issuer = templateDetails.getSignatories().get(0);
        String owner = templateDetails.getSignatories().get(1);
        upgrade.add("sig_issuer", issuer);
        upgrade.add("sig_owner", owner);
        return upgrade.render();
    }

    private static void populate(ST upgrade, String moduleName, String templateName, ArchiveDiffs archiveDiffs)
    {
        upgrade.add("module_name", moduleName);
        upgrade.add("contract_name", templateName);
        upgrade.add("fields", archiveDiffs.getFieldNamesInBoth(moduleName, templateName));
        upgrade.add("new_optional_fields", archiveDiffs.getAdditionalOptionalFields(moduleName, templateName));
    }

    private static String createInitiateUpgradeScript(String moduleName, TemplateDetails templateDetails)
    {
        ST upgrade = new ST(UPGRADE_INITIATE_SCRIPT);
        upgrade.add("module_name", moduleName);
        upgrade.add("contract_name", templateDetails.name());
        //todo - try to identify the actual owner
        String issuer = templateDetails.getSignatories().get(0);
        String owner = templateDetails.getSignatories().get(1);
        upgrade.add("sig_issuer", issuer);
        upgrade.add("sig_owner", owner);
        return upgrade.render();
    }

    public static String createProjectYaml(String sdkVersion, String archiveNameFrom, String archiveNameTo,
                                            String archiveDepFrom, String archiveDepTo, String dataDependencies)
    {
        ST upgrade = new ST(UPGRADE_PROJECT_YAML);
        upgrade.add("sdk_version", sdkVersion);
        upgrade.add("archive_name_v1", archiveNameFrom);
        upgrade.add("archive_name_v2", archiveNameTo);
        upgrade.add("archive_dep_v1", archiveDepFrom);
        upgrade.add("archive_dep_v2", archiveDepTo);
        String result = upgrade.render();

        if(dataDependencies != null)
        {
            result = result + "data-dependencies:\n" +
                    "  - " + dataDependencies;
        }

        return result;
    }
}

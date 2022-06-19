package org.jh.tools.daml;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

public class UpgradeTemplate
{
    private static final String UPGRADE_TEMPLATE = "module <module_name>.Upgrade<contract_name> where\n" +
            "\n" +
            "import qualified V1.<module_name> as <module_name>V1\n" +
            "import qualified V2.<module_name> as <module_name>V2\n" +
            "\n" +
            "template Upgrade<contract_name>Proposal\n" +
            "  with\n" +
            "    issuer : Party\n" +
            "    owner : Party\n" +
            "  where\n" +
            "    signatory issuer\n" +
            "    observer owner\n" +
            "    key (issuer, owner) : (Party, Party)\n" +
            "    maintainer key._1\n" +
            "    choice Accept : ContractId Upgrade<contract_name>Agreement\n" +
            "      controller owner\n" +
            "      do create Upgrade<contract_name>Agreement with ..\n" +
            "\n" +
            "template Upgrade<contract_name>Agreement\n" +
            "  with\n" +
            "    issuer : Party\n" +
            "    owner : Party\n" +
            "  where\n" +
            "    signatory issuer, owner\n" +
            "    key (issuer, owner) : (Party, Party)\n" +
            "    maintainer key._1\n" +
            "    nonconsuming choice Upgrade : ContractId <module_name>V2.<contract_name>\n" +
            "      with\n" +
            "        certId : ContractId <module_name>V1.<contract_name>\n" +
            "      controller issuer\n" +
            "      do cert \\<- fetch certId\n" +
            "         assert (cert.issuer == issuer)\n" +
            "         assert (cert.owner == owner)\n" +
            "         archive certId\n" +
            "         create <module_name>V2.<contract_name> with\n" +
            "<fields:{ field |           <field> = cert.<field>\n }>";

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

    public static List<Module> createUpgradeTemplatesContent(String moduleName, List<TemplateDetails> contractNames)
    {
        List<Module> contracts = new ArrayList<>();

        for(TemplateDetails templateDetails: contractNames)
        {
            String upgradeTemplate = createUpgradeTemplate(moduleName, templateDetails);
            String upgradeModuleName = "Upgrade" + templateDetails.name();
            contracts.add(new Module(upgradeModuleName, upgradeTemplate));
        }
        return contracts;
    }

    private static String createUpgradeTemplate(String moduleName, TemplateDetails templateDetails)
    {
        ST upgrade = new ST(UPGRADE_TEMPLATE);
        upgrade.add("module_name", moduleName);
        upgrade.add("contract_name", templateDetails.name());
        upgrade.add("fields",templateDetails.getFieldNames());
        return upgrade.render();
    }

    public static String createProjectYaml(String sdkVersion, String archiveNameFrom, String archiveNameTo)
    {
        return createProjectYaml(sdkVersion, archiveNameFrom, archiveNameTo, archiveNameFrom, archiveNameTo);
    }

    public static String createProjectYaml(String sdkVersion, String archiveNameFrom, String archiveNameTo,
                                            String archiveDepFrom, String archiveDepTo)
    {
        ST upgrade = new ST(UPGRADE_PROJECT_YAML);
        upgrade.add("sdk_version", sdkVersion);
        upgrade.add("archive_name_v1", archiveNameFrom);
        upgrade.add("archive_name_v2", archiveNameTo);
        upgrade.add("archive_dep_v1", archiveDepFrom);
        upgrade.add("archive_dep_v2", archiveDepTo);
        return upgrade.render();
    }
}

package org.jh.tools.daml;

import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;

public class UpgradeTemplate
{
    private static final String UPGRADE_TEMPLATE = "module Upgrade<contract_name> where\n" +
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
            "           issuer = cert.issuer\n" +
            "           owner = cert.owner\n" +
            "           carbon_metric_tons = cert.carbon_metric_tons\n" +
            "           carbon_offset_method = \"unknown\"";

    public static List<Module> createUpgradeTemplatesContent(String moduleName, List<String> contractNames)
    {
        List<Module> contracts = new ArrayList<>();

        for(String contractName: contractNames)
        {
            String upgradeTemplate = createUpgradeTemplate(moduleName, contractName);
            String upgradeModuleName = "Upgrade" + contractName;
            contracts.add(new Module(upgradeModuleName, upgradeTemplate));
        }
        return contracts;
    }

    private static String createUpgradeTemplate(String moduleName, String contractName)
    {
        ST upgrade = new ST(UPGRADE_TEMPLATE);
        upgrade.add("module_name", moduleName);
        upgrade.add("contract_name", contractName);
        return upgrade.render();
    }
}

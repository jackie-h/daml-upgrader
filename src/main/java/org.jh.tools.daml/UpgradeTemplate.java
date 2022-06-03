package org.jh.tools.daml;

import org.stringtemplate.v4.ST;

public class UpgradeTemplate
{
    private static final String UPGRADE_PROPOSAL = "template <proposal_contract_name>\n" +
            "  with\n" +
            "    issuer : Party\n" +
            "    owner : Party\n" +
            "  where\n" +
            "    signatory issuer\n" +
            "    observer owner\n" +
            "    key (issuer, owner) : (Party, Party)\n" +
            "    maintainer key._1\n" +
            "    choice Accept : ContractId <agreement_contract_name>\n" +
            "      controller owner\n" +
            "      do create <agreement_contract_name> with ..";

    private static final String UPGRADE_AGREEMENT = "template <agreement_contract_name>\n" +
            "  with\n" +
            "    issuer : Party\n" +
            "    owner : Party\n" +
            "  where\n" +
            "    signatory issuer, owner\n" +
            "    key (issuer, owner) : (Party, Party)\n" +
            "    maintainer key._1\n" +
            "    nonconsuming choice Upgrade : ContractId <new_contract_name>\n" +
            "      with\n" +
            "        certId : ContractId <current_contract_name>\n" +
            "      controller issuer\n" +
            "      do cert \\<- fetch certId\n" +
            "         assert (cert.issuer == issuer)\n" +
            "         assert (cert.owner == owner)\n" +
            "         archive certId\n" +
            "         create <new_contract_name> with\n" +
            "           issuer = cert.issuer\n" +
            "           owner = cert.owner\n" +
            "           carbon_metric_tons = cert.carbon_metric_tons\n" +
            "           carbon_offset_method = \"unknown\"";

    public static scala.Tuple2<String, String> createUpgradeTemplatesContent(String contractName)
    {
        String proposal = createUpgradeProposal(contractName);
        String agreement = createUpgradeAgreement(contractName);
        return new scala.Tuple2<>(proposal,agreement);
    }

    private static String createUpgradeProposal(String contractName)
    {
        String proposalContractName = proposalContractName(contractName);
        String agreementContractName = agreementContractName(contractName);

        ST proposal = new ST(UPGRADE_PROPOSAL);
        proposal.add("proposal_contract_name", proposalContractName);
        proposal.add("agreement_contract_name", agreementContractName);

        return proposal.render();
    }

    private static String createUpgradeAgreement(String contractName)
    {
        String agreementContractName = agreementContractName(contractName);

        String oldName = contractName;
        String newName = contractName;

        ST agreement = new ST(UPGRADE_AGREEMENT);
        agreement.add("agreement_contract_name", agreementContractName);
        agreement.add("current_contract_name", oldName);
        agreement.add("new_contract_name", newName);
        return agreement.render();
    }

    private static String proposalContractName(String contractName)
    {
        return "Upgrade" + contractName + "Proposal";
    }

    private static String agreementContractName(String contractName)
    {
        return "Upgrade" + contractName + "Agreement";
    }
}

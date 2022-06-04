package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.lf.archive.ArchivePayload;
import com.daml.lf.archive.Reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Upgrader
{
    private static final Logger LOGGER =  Logger.getLogger(Upgrader.class.getName());

    public static List<String> createUpgrades(String currentArchivePath, String newArchivePath, String outputPath)
    {
        LOGGER.info("Starting upgrade");
        LOGGER.info("Current Archive Path=" + currentArchivePath);
        LOGGER.info("New Archive Path=" + newArchivePath);

        DamlLf.Archive archiveOld = Dar.readDar(currentArchivePath).getDamlLf();
        DamlLf.Archive archiveNew = Dar.readDar(newArchivePath).getDamlLf();

        List<String> upgrades = identifyTemplatesToUpgrade(archiveOld, archiveNew);
        createAndWriteUpgradesToFiles(upgrades, outputPath);
        return upgrades;
    }

    private static List<String> identifyTemplatesToUpgrade(DamlLf.Archive archiveCurrent,
                                                           DamlLf.Archive archiveNew)
    {
        LOGGER.info(archiveCurrent.getHash());
        LOGGER.info(archiveNew.getHash());

        if (archiveCurrent.getHash().equals(archiveNew.getHash()))
        {
            LOGGER.info("Contents identical nothing to do");
            return new ArrayList<>();
        }

        ArchivePayload payloadCurrent = Reader.readArchive(archiveCurrent).right().get();
        ArchivePayload payloadNew = Reader.readArchive(archiveNew).right().get();
        LOGGER.info(payloadCurrent.pkgId());
        LOGGER.info(payloadNew.pkgId());

        return DamlLfProtoUtils.findTemplatesThatAreInOneButDifferentInTwo(payloadCurrent.proto(), payloadNew.proto());
    }

    private static void createAndWriteUpgradesToFiles(List<String> upgrades, String outpath)
    {
        for(String contractName : upgrades)
        {
            createAndWriteUpgradeToFiles(contractName, outpath);
        }
    }

    private static void createAndWriteUpgradeToFiles(String contractName, String outpath)
    {
        java.util.Map<String, String> contracts = UpgradeTemplate.createUpgradeTemplatesContent(contractName);

        for (String upgradeContractName : contracts.keySet())
        {
            String fileName = outpath + "/" + upgradeContractName + ".daml";
            Path filePath = Paths.get(fileName);

            try
            {
                Files.writeString(filePath, contracts.get(upgradeContractName));
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new RuntimeException("Failed to create contract:" + upgradeContractName);
            }
        }
    }

}

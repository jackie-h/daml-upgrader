package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.lf.archive.ArchivePayload;
import com.daml.lf.archive.Reader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Upgrader
{
    private static final Logger LOGGER =  Logger.getLogger(Upgrader.class.getName());

    public static Map<String, List<String>> createUpgrades(String currentArchivePath, String newArchivePath, String outputPath)
    {
        LOGGER.info("Starting upgrade");
        LOGGER.info("Current Archive Path=" + currentArchivePath);
        LOGGER.info("New Archive Path=" + newArchivePath);

        DamlLf.Archive archiveOld = Dar.readDar(currentArchivePath).getDamlLf();
        DamlLf.Archive archiveNew = Dar.readDar(newArchivePath).getDamlLf();

        Map<String, List<String>> upgrades = identifyTemplatesToUpgrade(archiveOld, archiveNew);
        createAndWriteUpgradesToFiles(upgrades, outputPath);
        return upgrades;
    }

    private static Map<String, List<String>> identifyTemplatesToUpgrade(DamlLf.Archive archiveCurrent,
                                                                        DamlLf.Archive archiveNew)
    {
        LOGGER.info(archiveCurrent.getHash());
        LOGGER.info(archiveNew.getHash());

//        if (archiveCurrent.getHash().equals(archiveNew.getHash()))
//        {
//            LOGGER.info("Contents identical nothing to do");
//            return new ArrayList<>();
//        }

        ArchivePayload payloadCurrent = Reader.readArchive(archiveCurrent).right().get();
        ArchivePayload payloadNew = Reader.readArchive(archiveNew).right().get();
        LOGGER.info(payloadCurrent.pkgId());
        LOGGER.info(payloadNew.pkgId());

        return DamlLfProtoUtils.findTemplatesThatAreInOneButDifferentInTwo(payloadCurrent.proto(), payloadNew.proto());
    }

    private static void createAndWriteUpgradesToFiles(Map<String,List<String>> upgrades, String outpath)
    {
        for(String moduleName : upgrades.keySet())
        {
            List<String> contractNames = upgrades.get(moduleName);
            createAndWriteUpgradeToFiles(moduleName, contractNames, outpath);
        }
    }

    private static void createAndWriteUpgradeToFiles(String moduleName, List<String> contractNames, String outpath)
    {
        java.util.Map<String, String> contracts = UpgradeTemplate.createUpgradeTemplatesContent(moduleName, contractNames);

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

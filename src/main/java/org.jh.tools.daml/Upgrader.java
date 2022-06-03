package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.lf.archive.ArchivePayload;
import com.daml.lf.archive.Decode;
import com.daml.lf.archive.Reader;
import com.daml.lf.data.Ref;
import com.daml.lf.language.Ast;
import scala.Tuple2;
import scala.collection.immutable.Map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Upgrader
{
    private static final Logger LOGGER =  Logger.getLogger(Upgrader.class.getName());

    public static void createUpgrades(String currentArchivePath, String newArchivePath, String outputPath)
    {
        LOGGER.info("Starting upgrade");
        LOGGER.info("Current Archive Path=" + currentArchivePath);
        LOGGER.info("New Archive Path=" + newArchivePath);

        DamlLf.Archive archiveOld = readDar(currentArchivePath);
        DamlLf.Archive archiveNew = readDar(newArchivePath);

        List<String> upgrades = identifyTemplatesToUpgrade(archiveOld, archiveNew);

        createAndWriteUpgradesToFiles(upgrades, outputPath);
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

        Tuple2<String, Ast.GenPackage<Ast.Expr>> out = Decode.decodeArchivePayload(payloadCurrent, false).right().get();
        LOGGER.info(out._1);

        Map<Ref.DottedName, Ast.GenModule<Ast.Expr>> modules = out._2.modules();
        LOGGER.info(out._2.modules().keySet().mkString(","));

        return new ArrayList<>();
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

    private static DamlLf.Archive readDar(String filePath)
    {
        Path path = Paths.get(filePath);

        try
        {
            ZipInputStream is = new ZipInputStream(java.nio.file.Files.newInputStream(path));
            ZipEntry entry = is.getNextEntry();
            LOGGER.info(entry.getName());
            byte[] bytes = is.readAllBytes();
            DamlLf.Archive archiveProto = DamlLf.Archive.parseFrom(bytes);
            LOGGER.info(archiveProto.getHash());

            return archiveProto;
        }
        catch (IOException e)
        {
            //todo: clean-up
            e.printStackTrace();
            throw new RuntimeException("Failed to read archive:" + filePath);
        }

    }
}

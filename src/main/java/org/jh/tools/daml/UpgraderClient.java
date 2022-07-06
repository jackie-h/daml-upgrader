package org.jh.tools.daml;

public class UpgraderClient
{
    public static void main(String[] args)
    {
        String filePath1 = args[0];
        String filePath2 = args[1];
        String outputDirectory = args[2];
        String dataDirectory = null;

        if(args.length == 4)
        {
            dataDirectory = args[4];
        }

        Upgrader.createUpgrades(filePath1, filePath2, outputDirectory, dataDirectory);
    }
}

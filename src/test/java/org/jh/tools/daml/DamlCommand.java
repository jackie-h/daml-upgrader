package org.jh.tools.daml;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class DamlCommand
{
    public static void cleanBuildDar(String directory) throws IOException
    {
        File dir = Paths.get(directory).toFile();
        exec("daml clean",  dir);
        exec("daml build --log-level ERROR",  dir);
    }

    public static String exec(String cmd) throws IOException
    {
        return exec(cmd, null);
    }

    public static String exec(String cmd, File dir) throws IOException
    {
        String result = null;
        Process proc = (dir == null) ? Runtime.getRuntime().exec(cmd): Runtime.getRuntime().exec(cmd, new String[]{}, dir);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        boolean exited = false;
        try
        {
            exited = proc.waitFor(10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }



        //proc.getOutputStream()

        // Read any errors from the attempted command
        StringBuilder err = new StringBuilder();
        String e = null;
        while (stdError.ready() && (e = stdError.readLine()) != null) {
            err.append(e).append("\n");
        }
        Assert.assertEquals("", err.toString());

        // Read the output from the command
        String s = null;
        StringBuilder out = new StringBuilder();
        while ((s = stdInput.readLine()) != null) {
            out.append(s).append("\n");
        }

        return out.toString();
    }
}

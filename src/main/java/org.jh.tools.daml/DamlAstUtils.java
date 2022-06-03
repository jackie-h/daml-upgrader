package org.jh.tools.daml;

import com.daml.lf.archive.ArchivePayload;
import com.daml.lf.archive.Decode;
import com.daml.lf.data.Ref;
import com.daml.lf.language.Ast;
import scala.Tuple2;
import scala.collection.JavaConverters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DamlAstUtils
{
    private static final Logger LOGGER = Logger.getLogger(DamlAstUtils.class.getName());

    public static void compareAsts(ArchivePayload payloadCurrent, ArchivePayload payloadNew)
    {
        List<String> names = new ArrayList<>();

        Tuple2<String, Ast.GenPackage<Ast.Expr>> currentPackageAst = Decode.decodeArchivePayload(payloadCurrent, false).right().get();
        LOGGER.info(currentPackageAst._1);
        Tuple2<String, Ast.GenPackage<Ast.Expr>> newPackageAst = Decode.decodeArchivePayload(payloadNew, false).right().get();


        if (!currentPackageAst.equals(newPackageAst))
        {
            LOGGER.info(currentPackageAst._2.modules().keySet().mkString(","));

            Map<Ref.DottedName, Ast.GenModule<Ast.Expr>> currentModules = scala.collection.JavaConverters.mapAsJavaMapConverter(currentPackageAst._2.modules()).asJava();
            Map<Ref.DottedName, Ast.GenModule<Ast.Expr>> newModules = scala.collection.JavaConverters.mapAsJavaMapConverter(newPackageAst._2.modules()).asJava();

            for (Map.Entry<Ref.DottedName, Ast.GenModule<Ast.Expr>> entry : currentModules.entrySet())
            {
                Ref.DottedName moduleName = entry.getKey();
                Ast.GenModule<Ast.Expr> currentModule = entry.getValue();

                Ast.GenModule<Ast.Expr> newModule = newModules.get(entry.getKey());
                if (newModule != null)
                {
                    LOGGER.info("Found module in current and new:" + currentModule.name().dottedName());

                    Map<Ref.DottedName, Ast.GenTemplate<Ast.Expr>> currentTemplates = JavaConverters.mapAsJavaMapConverter(currentModule.templates()).asJava();
                    Map<Ref.DottedName, Ast.GenTemplate<Ast.Expr>> newTemplates = JavaConverters.mapAsJavaMapConverter(newModule.templates()).asJava();

                    for (Ref.DottedName name : currentTemplates.keySet())
                    {
                        Ast.GenTemplate<Ast.Expr> currentTemplate = currentTemplates.get(name);
                        Ast.GenTemplate<Ast.Expr> newTemplate = newTemplates.get(name);

                        if (newTemplate != null && !currentTemplate.equals(newTemplate))
                        {
                            LOGGER.info("Template has changes:" + name.dottedName());
                            names.add(name.dottedName());
                        }
                    }
                }
            }
        }
    }
}

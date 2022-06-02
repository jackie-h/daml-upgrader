package org.jh.tools.daml;

import com.daml.daml_lf_dev.DamlLf;
import com.daml.lf.archive.*;
import com.daml.lf.archive.Error;
import com.daml.lf.data.Ref;
import com.daml.lf.language.Ast;
import com.google.protobuf.CodedInputStream;
import scala.Function1;
import scala.Tuple2;
import scala.collection.immutable.Map;
import scala.util.Either;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Upgrader {
    public static void main(String[] args) {

        String filePath1 = args[0];
        String filePath2 = args[1];

        System.out.println("Starting upgrade");

        readDar(filePath1);
    }

    private static void readDar(String filePath) {
        Path path = Paths.get(filePath);

        try {

            ZipInputStream is = new ZipInputStream(java.nio.file.Files.newInputStream(path));
            ZipEntry entry = is.getNextEntry();
            System.out.println(entry.getName());

            byte[] bytes = is.readAllBytes();

            //DarManifestReader.dalfNames(bytes);

            System.out.println("here");
            DamlLf.Archive archiveProto = DamlLf.Archive.parseFrom(bytes);
            System.out.println(archiveProto.getHash());

            ArchivePayload payload = Reader.readArchive(archiveProto).right().get();
            System.out.println(payload.pkgId());
            //payload.proto().getDamlLf1().modules_

            Tuple2<String, Ast.GenPackage<Ast.Expr>> out = Decode.decodeArchivePayload(payload, false).right().get();
            System.out.println(out._1);

            Map<Ref.DottedName, Ast.GenModule<Ast.Expr>> modules = out._2.modules();
            System.out.println(out._2.modules().keySet().mkString(","));


        } catch (IOException e) {
            e.printStackTrace();
        }

        //com.daml.lf.archive.UniversalArchiveDecoder

//        try {
//            //java.nio.file.Files.newInputStream(path);
//            //GenReader genReader = new GenReader(new F)
//
//
//            GenReader genReader = new GenReader(new Function1<CodedInputStream, Either>() {
//                @Override
//                public Either apply(CodedInputStream v1) {
//                    try {
//                        new Either<>()
//                        return CodedInputStream.newInstance(java.nio.file.Files.newInputStream(path));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    return null;
//                }
//
//                @Override
//                public <A> Function1<A, Either> compose(Function1<A, CodedInputStream> g) {
//                    return Function1.super.compose(g);
//                }
//
//                @Override
//                public <A> Function1<CodedInputStream, A> andThen(Function1<Either, A> g) {
//                    return Function1.super.andThen(g);
//                }
//            });
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        if (maybeReader.isRight())
//        {
//            GenReader genReader = maybeReader.right().get();
//            GenUniversalArchiveReader reader = new GenUniversalArchiveReader(genReader);
//        }

    }
}

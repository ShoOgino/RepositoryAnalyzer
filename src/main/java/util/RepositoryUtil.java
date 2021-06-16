package util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class RepositoryUtil {
    public static void checkoutRepository(Repository repository, String idCommit) throws IOException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
        /*
        //repositoryについて、そのコミットidへcheckout。
        System.out.println("checkout repository");
        ProcessBuilder pbCheckout = new ProcessBuilder("git", "checkout", "-f", idCommit );
        pbCheckout.redirectErrorStream(true);  // 標準エラー出力の内容を標準出力にマージする

        File dir = new File(pathRepository);
        pbCheckout.directory(dir);
        Process process = pbCheckout.start();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println(line);
            }
        }
         */
        System.out.println("chackout at "+idCommit+"...");
        Git git = new Git(repository);
        git.clean().setForce(true).call();
        git.reset().setMode(ResetCommand.ResetType.HARD).call();
        git.checkout().setForced(true).setName(idCommit).call();
    }
}

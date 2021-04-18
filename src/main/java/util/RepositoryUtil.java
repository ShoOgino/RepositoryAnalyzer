package util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.*;

import java.io.File;
import java.io.IOException;

public class RepositoryUtil {
    public static void checkoutRepository(String pathRepository, String idCommit) throws IOException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
        //repositoryについて、そのコミットidへcheckout。
        System.out.println("chackout at "+idCommit+"...");
        Git git = Git.open(new File(pathRepository));
        git.reset().setMode(ResetCommand.ResetType.HARD).call();
        git.clean().call();
        git.checkout().setName(idCommit).call();
    }
}

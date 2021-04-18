package data;

import me.tongfei.progressbar.ProgressBar;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestModules {
    public static String   pathProject   = "C:/Users/ShoOgino/data/workspace/MLTool/datasets/egit";
    public static String   idCommitHead = "b459d7381ea57e435bd9b71eb37a4cb4160e252b";
    public static String[] commitEdgesMethod = {"2c1b0f4ad24fb082e5eb355e912519c21a5e3f41", "1241472396d11fe0e7b31c6faf82d04d39f965a6"};
    public static String[] commitEdgesFile =  {"dfbdc456d8645fc0c310b5e15cf8d25d8ff7f84b","0cc8d32aff8ce91f71d2cdac8f3e362aff747ae7"};

    public static String pathRepositoryMethod = pathProject+"/repositoryMethod";
    public static String pathRepositoryFile = pathProject+"/repositoryFile";
    public static String pathDataset = pathProject+"/datasets/"+ commitEdgesMethod[0].substring(0,8)+"_"+ commitEdgesMethod[1].substring(0,8)+".csv";
    public static String pathModules = pathProject+"/modules.json";
    public static String pathCommits = pathProject+"/commits";
    public static String pathBugs = pathProject+"/bugs.json";

    public static Commits commitsAll = new Commits();
    public static Modules modulesAll = new Modules();
    public static Bugs bugsAll = new Bugs();

    @BeforeAll
    static public void setUp() throws GitAPIException, IOException {
        util.RepositoryUtil.checkoutRepository(pathRepositoryFile, commitEdgesFile[1]);
        util.RepositoryUtil.checkoutRepository(pathRepositoryMethod, commitEdgesMethod[1]);
        commitsAll.loadCommits(pathRepositoryMethod, idCommitHead);
        modulesAll.analyzeModules(commitsAll);
    }

    @Test
    public void testIdentifyCommitsParent() throws Exception {
        for(Module module: ProgressBar.wrap(modulesAll.values(), "testidentifyparents")){
            for(Modification modification: module.modifications.values()){
                if(modification.sourceOld==null) continue;
                boolean isParentOk = false;
                for(String idCommitParent: modification.parents){
                    List<Modification> modificationsBefore = module.modifications.get(idCommitParent);
                    for(Modification modificationBefore: modificationsBefore){
                        if(modification.sourceOld.equals(modificationBefore.sourceNew)){
                            isParentOk=true;
                        }
                    }
                }
                if(isParentOk)continue;
                System.out.println(module.path);
                System.out.println(modification.idCommit);
                System.out.println(modification.sourceOld);
                for(String idCommitParent: modification.parents){
                    List<Modification> modificationsBefore = module.modifications.get(idCommitParent);
                    for(Modification modificationBefore: modificationsBefore) {
                        System.out.println(modificationBefore.idCommit);
                        System.out.println(modificationBefore.sourceNew);
                    }
                }
            }
        }
    }
}

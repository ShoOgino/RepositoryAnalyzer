# AnalyzeRepository
## environment
- jdk(openjdk version "11.0.11" 2021-04-20で動作確認済み)
## quickstart
オープンソースプロジェクトであるEGitを対象に、メソッドについてのメトリクスをAnalyzeRepositoryを用いて計測してみる。
### 1. AnalyzeRepositoryをビルド
1. git clone https://github.com/ShoOgino/AnalyzeRepository.git
2. AnalyzeRepositoryのフォルダ直下に移動し、`./gradlew shadowJar`で実行可能jarをビルド。

### 2. Javaプロジェクトのデータを用意する。
1. プロジェクトフォルダを作成する(例: projectEGit)。
2. プロジェクトフォルダ直下で下記の操作を行なう。
    - git clone https://github.com/eclipse/egit.git repositoryFile
    - git clone https://github.com/ShoOgino/egitMethod202104.git repositoryMethod
    - https://github.com/ShoOgino/bugs/blob/main/egit/bugs.jsonをダウンロード
- プロジェクトフォルダのディレクトリ構成は下記の通りになっているはず。
    - projectEgit
        - repositoryMethod(メソッド粒度のリポジトリフォルダ)
        - repositoryFile(ファイル粒度のリポジトリフォルダ)
        - bugs.json

### 3. プロジェクトに含まれるメソッドについて各種メトリクスを算出。
1. 下記のコマンドを入力。
- java -jar ${ビルドされたjar}<br>
    --pathProject ${プロジェクトフォルダのパス}<br>
    --idCommitHead ${headコミットid(メソッド粒度の方)}<br>
    --commitEdgesMethod ${コミットid(メソッド粒度の方)。対象期間の始端を表す} ${コミットid(メソッド粒度の方)。対象期間の終端を表す} <br>
    --commitEdgesFile ${コミットid(ファイル粒度の方)。対象期間の始端を表す} ${コミットid(ファイル粒度の方)。対象期間の終端を表す}<br>
    --calcMetrics<br>
    を実行。
- 結果として、下記のようなディレクトリ構成になっているはず。
    - ${プロジェクトフォルダ}
        - repositoryMethod(メソッド粒度のリポジトリフォルダ)
        - repositoryFile(ファイル粒度のリポジトリフォルダ)
        - datasets
            - ${対象期間のメソッドについてのデータセット}.csv
        - bugs.json

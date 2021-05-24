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
1. 下記のコマンドを入力。自分の環境に合った内容を入力すること。
    - java -jar ビルドされた.jarのパス<br>
        --pathProject プロジェクトフォルダのパス<br>
        --idCommitHead b459d7381ea57e435bd9b71eb37a4cb4160e252b <br>
        --commitEdgesMethod 2c1b0f4ad24fb082e5eb355e912519c21a5e3f41 1241472396d11fe0e7b31c6faf82d04d39f965a6 <br>
        --commitEdgesFile dfbdc456d8645fc0c310b5e15cf8d25d8ff7f84b 0cc8d32aff8ce91f71d2cdac8f3e362aff747ae7<br>
        --calcMetrics<br>
- 結果として、下記のようなディレクトリ構成になっているはず。
    - projectEGit
        - repositoryMethod(メソッド粒度のリポジトリフォルダ)
        - repositoryFile(ファイル粒度のリポジトリフォルダ)
        - datasets
            - 2c1b0f4a_12414723.csv
        - bugs.json

#!/usr/bin/env bash
# 잔디 빌드 스크립트: 컴파일 → 자체 테스트 → 실행 jar → API 문서(Javadoc)
# 필요: JDK 17 이상 (javac, jar, javadoc), git
set -euo pipefail
export LC_ALL=${LC_ALL:-C.UTF-8}
cd "$(dirname "$0")"

OUT=build
VERSION="0.1"

# 클래스패스 구분자: Windows(Git Bash 등)의 java는 ';', 나머지는 ':'
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) SEP=';' ;;
  *)                    SEP=':' ;;
esac

# Windows는 실행 중인 jar를 잠그므로, 잔디가 켜져 있으면 build 폴더를 지울 수 없다
rm -rf "$OUT" 2>/dev/null || {
  echo "오류: $OUT 폴더를 지울 수 없습니다. 잔디가 실행 중이면 끄고 다시 빌드하세요." >&2
  exit 1
}
mkdir -p "$OUT/classes" "$OUT/test-classes" "$OUT/dist"

echo "[1/4] 컴파일"
javac -encoding UTF-8 --release 17 -Xlint:all,-serial,-this-escape -d "$OUT/classes" \
      $(find src/main/java -name '*.java')
cp -r src/main/resources/. "$OUT/classes/"

echo "[2/4] 자체 테스트"
javac -encoding UTF-8 --release 17 -cp "$OUT/classes" -d "$OUT/test-classes" \
      $(find src/test/java -name '*.java')
java -Dstdout.encoding=UTF-8 -Djava.awt.headless=true -cp "$OUT/classes${SEP}$OUT/test-classes" jandi.SelfTest

echo "[3/4] 실행 jar"
cat > "$OUT/MANIFEST.MF" <<MF
Main-Class: jandi.App
Implementation-Title: jandi
Implementation-Version: $VERSION
Implementation-Vendor: Team 3
MF
jar --create --file "$OUT/dist/jandi.jar" --manifest "$OUT/MANIFEST.MF" -C "$OUT/classes" .

echo "[4/4] API 문서"
javadoc -quiet -encoding UTF-8 -charset UTF-8 -docencoding UTF-8 -locale ko_KR \
        -author -version -Xdoclint:all,-missing \
        -windowtitle "잔디 $VERSION API" -doctitle "잔디 $VERSION API 문서" \
        -bottom "2026 소프트웨어시스템설계 팀 프로젝트 · Team 3" \
        -overview src/main/javadoc/overview.html -d "$OUT/javadoc" -sourcepath src/main/java -subpackages jandi

echo
echo "완료: $OUT/dist/jandi.jar , $OUT/javadoc/index.html"
echo "실행: java -jar $OUT/dist/jandi.jar   (샘플: --sample, git 폴더: --repo <폴더>, 데이터 폴더: --data <폴더>)"

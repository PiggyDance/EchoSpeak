#!/bin/bash

# EchoSpeak 国际化资源验证脚本
# 检查所有语言的资源文件是否完整且格式正确

set -e

echo "🔍 EchoSpeak 国际化资源验证"
echo "============================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 计数器
total_checks=0
passed_checks=0
failed_checks=0

# 资源文件路径
RES_DIR="composeApp/src/androidMain/res"

# 必需的字符串资源 ID
REQUIRED_STRINGS=(
    "app_name"
    "status_recording"
    "status_playing"
    "status_idle"
    "mode_idle"
    "mode_recording"
    "mode_playing"
)

# 语言文件夹
LANGUAGE_FOLDERS=(
    "values"
    "values-zh-rCN"
    "values-zh-rTW"
    "values-ja"
    "values-ko"
    "values-fr"
    "values-es"
    "values-ru"
)

# 检查函数
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((passed_checks++))
    ((total_checks++))
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
    ((failed_checks++))
    ((total_checks++))
}

echo -e "${BLUE}1. 检查资源文件夹存在性${NC}"
echo "--------------------------------"

for folder in "${LANGUAGE_FOLDERS[@]}"; do
    if [ -d "$RES_DIR/$folder" ]; then
        check_pass "$folder 文件夹存在"
    else
        check_fail "$folder 文件夹不存在"
    fi
done

echo ""
echo -e "${BLUE}2. 检查 strings.xml 文件${NC}"
echo "--------------------------------"

for folder in "${LANGUAGE_FOLDERS[@]}"; do
    strings_file="$RES_DIR/$folder/strings.xml"
    if [ -f "$strings_file" ]; then
        check_pass "$folder/strings.xml 文件存在"
    else
        check_fail "$folder/strings.xml 文件不存在"
    fi
done

echo ""
echo -e "${BLUE}3. 检查必需的字符串资源${NC}"
echo "--------------------------------"

for folder in "${LANGUAGE_FOLDERS[@]}"; do
    strings_file="$RES_DIR/$folder/strings.xml"
    
    if [ -f "$strings_file" ]; then
        echo ""
        echo -e "${YELLOW}检查 $folder:${NC}"
        
        for string_id in "${REQUIRED_STRINGS[@]}"; do
            if grep -q "name=\"$string_id\"" "$strings_file"; then
                check_pass "  $string_id 存在"
            else
                check_fail "  $string_id 缺失"
            fi
        done
    fi
done

echo ""
echo -e "${BLUE}4. 检查 XML 格式${NC}"
echo "--------------------------------"

for folder in "${LANGUAGE_FOLDERS[@]}"; do
    strings_file="$RES_DIR/$folder/strings.xml"
    
    if [ -f "$strings_file" ]; then
        # 检查 XML 声明
        if head -n 1 "$strings_file" | grep -q "<?xml version=\"1.0\" encoding=\"utf-8\"?>"; then
            check_pass "$folder XML 声明正确"
        else
            check_fail "$folder XML 声明缺失或错误"
        fi
        
        # 检查根元素
        if grep -q "<resources>" "$strings_file" && grep -q "</resources>" "$strings_file"; then
            check_pass "$folder 根元素正确"
        else
            check_fail "$folder 根元素缺失或错误"
        fi
    fi
done

echo ""
echo -e "${BLUE}5. 检查代码文件${NC}"
echo "--------------------------------"

# 检查核心文件
code_files=(
    "composeApp/src/commonMain/kotlin/io/piggydance/echospeak/StringResources.kt"
    "composeApp/src/androidMain/kotlin/io/piggydance/echospeak/StringResourcesAndroid.kt"
    "composeApp/src/iosMain/kotlin/io/piggydance/echospeak/StringResourcesIos.kt"
)

for file in "${code_files[@]}"; do
    if [ -f "$file" ]; then
        check_pass "$(basename $file) 存在"
    else
        check_fail "$(basename $file) 不存在"
    fi
done

echo ""
echo -e "${BLUE}6. 检查文档文件${NC}"
echo "--------------------------------"

doc_files=(
    "docs/INTERNATIONALIZATION.md"
    "docs/HOW_TO_TEST_I18N.md"
    "docs/LANGUAGE_COMPARISON.md"
    "docs/I18N_IMPLEMENTATION_SUMMARY.md"
    "docs/I18N_CHECKLIST.md"
)

for file in "${doc_files[@]}"; do
    if [ -f "$file" ]; then
        check_pass "$(basename $file) 存在"
    else
        check_fail "$(basename $file) 不存在"
    fi
done

echo ""
echo "============================"
echo -e "${BLUE}验证结果总结${NC}"
echo "============================"
echo -e "总检查项: ${BLUE}$total_checks${NC}"
echo -e "通过: ${GREEN}$passed_checks${NC}"
echo -e "失败: ${RED}$failed_checks${NC}"
echo ""

if [ $failed_checks -eq 0 ]; then
    echo -e "${GREEN}🎉 所有检查通过! 国际化资源完整且格式正确。${NC}"
    exit 0
else
    echo -e "${RED}⚠️  发现 $failed_checks 个问题,请检查并修复。${NC}"
    exit 1
fi

#!/bin/bash

# EchoSpeak 多语言测试脚本
# 用法: ./scripts/test_languages.sh

set -e

echo "🌍 EchoSpeak 多语言测试工具"
echo "=============================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查 ADB 是否可用
if ! command -v adb &> /dev/null; then
    echo -e "${RED}❌ 错误: 未找到 ADB 命令${NC}"
    echo "请确保 Android SDK 已安装并将 platform-tools 添加到 PATH"
    exit 1
fi

# 检查是否有设备连接
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}❌ 错误: 未检测到 Android 设备${NC}"
    echo "请连接 Android 设备或启动模拟器"
    exit 1
fi

echo -e "${GREEN}✅ ADB 已就绪${NC}"
echo ""

# 语言列表
declare -A LANGUAGES=(
    ["en-US"]="🇺🇸 英语"
    ["zh-CN"]="🇨🇳 简体中文"
    ["zh-TW"]="🇹🇼 繁体中文"
    ["ja-JP"]="🇯🇵 日语"
    ["ko-KR"]="🇰🇷 韩语"
    ["fr-FR"]="🇫🇷 法语"
    ["es-ES"]="🇪🇸 西班牙语"
    ["ru-RU"]="🇷🇺 俄语"
)

# 显示当前语言
current_locale=$(adb shell getprop persist.sys.locale | tr -d '\r')
echo -e "${BLUE}📱 当前设备语言:${NC} $current_locale"
echo ""

# 主菜单
echo "请选择操作:"
echo "1) 切换到指定语言"
echo "2) 遍历所有语言测试"
echo "3) 恢复到英语"
echo "4) 退出"
echo ""
read -p "请输入选项 (1-4): " choice

case $choice in
    1)
        echo ""
        echo "支持的语言:"
        i=1
        declare -a locale_array
        for locale in "${!LANGUAGES[@]}"; do
            echo "$i) ${LANGUAGES[$locale]} ($locale)"
            locale_array[$i]=$locale
            ((i++))
        done
        echo ""
        read -p "请选择语言 (1-8): " lang_choice
        
        if [ "$lang_choice" -ge 1 ] && [ "$lang_choice" -le 8 ]; then
            selected_locale=${locale_array[$lang_choice]}
            echo ""
            echo -e "${YELLOW}⏳ 正在切换到 ${LANGUAGES[$selected_locale]} ($selected_locale)...${NC}"
            adb shell "setprop persist.sys.locale $selected_locale; setprop ctl.restart zygote"
            echo -e "${GREEN}✅ 语言已切换! 设备将重启...${NC}"
        else
            echo -e "${RED}❌ 无效的选择${NC}"
        fi
        ;;
    
    2)
        echo ""
        echo -e "${YELLOW}🔄 开始遍历所有语言...${NC}"
        echo "每种语言将停留 10 秒供您测试"
        echo ""
        
        for locale in "${!LANGUAGES[@]}"; do
            echo -e "${BLUE}===================================================${NC}"
            echo -e "${YELLOW}切换到: ${LANGUAGES[$locale]} ($locale)${NC}"
            echo -e "${BLUE}===================================================${NC}"
            
            adb shell "setprop persist.sys.locale $locale; setprop ctl.restart zygote"
            
            echo "⏱️  等待 10 秒..."
            echo "请在设备上检查:"
            echo "  - 应用名称是否正确"
            echo "  - 状态文本是否显示正确语言"
            echo "  - UI 布局是否正常"
            echo ""
            
            sleep 10
        done
        
        echo ""
        echo -e "${GREEN}✅ 所有语言测试完成!${NC}"
        echo -e "${YELLOW}💡 提示: 设备当前语言为最后测试的语言${NC}"
        ;;
    
    3)
        echo ""
        echo -e "${YELLOW}⏳ 正在恢复到英语 (en-US)...${NC}"
        adb shell "setprop persist.sys.locale en-US; setprop ctl.restart zygote"
        echo -e "${GREEN}✅ 已恢复到英语! 设备将重启...${NC}"
        ;;
    
    4)
        echo ""
        echo "👋 再见!"
        exit 0
        ;;
    
    *)
        echo -e "${RED}❌ 无效的选项${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}ℹ️  提示:${NC}"
echo "- 更改语言后设备会重启 (zygote 进程)"
echo "- 请在重启后手动打开 EchoSpeak 应用"
echo "- 如需测试其他语言,请重新运行此脚本"
echo ""

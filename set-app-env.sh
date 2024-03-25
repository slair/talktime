#!/bin/sh
#-*- coding: utf-8 -*-

# Exit the script on errors:
set -e
trap 'echo "$0 FAILED at line ${LINENO}"' ERR
# Catch unitialized variables:
set -u

# И ещё — в пару переменных забьём наши пакеты и классы.
# Если заходите их сменить — вам не придётся
# бегать по коду — все настройки вначале.

export AUTHOR_NAMESPACE=slairium

export APK_NAME=$(basename `pwd`)
export PACKAGE_PATH=com/${AUTHOR_NAMESPACE}/${APK_NAME}
export PACKAGE=com.${AUTHOR_NAMESPACE}.${APK_NAME}
export MAIN_CLASS=MainActivity

nf="bin obj"
for folder in ${nf}; do
	if [ ! -d ${folder} ]; then
		echo "Creating ${folder}"
		mkdir ${folder}
	fi
done

#~ env|sort

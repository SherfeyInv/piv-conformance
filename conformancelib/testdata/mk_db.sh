#!/usr/bin/bash

# Note: If this is running out of Cygwin, do NOT use this.
#
# Instead, use mk_db.bat from a true CMD window.  As of 5/7/2019,
# Cygwin's Python 2.7 implementation doesn't have the prequisite
# modules needed to convert .xlsx to .sql, so we must use the
# Windows 64 Python distro.

if [ $(expr $(uname) : "^.*CYG") -eq 3 ]; then
	echo -n "Hit <ENTER> when ready to invoke Windows Python detour around Cygwin: " 
	read ans
	mkdbParam=""
	if [ ! -d venv-xlrd ]; then #prompt create new
		echo "Forcing a new installation of venv-xlrd..."
		mkdbParam="-f"
	elif [ -d venv-xlrd ] && [ "z$1" == "z-f" ]; then
		echo "You must first manually remove venv-xlrd -- that's above my paygrade"
		exit 1
	elif [ "z$1" != "z-f" ]; then
		echo "Building databases..."
	fi
	cmd /c mk_db.bat $mkdbParam
	echo -n "Hit <ENTER> to close this window: " 
	read ans
	exit 1
fi

PYTHON=${PYTHON:-$(which python)}

# Don't try to parse $PS1, just go for it
$PYTHON -mvenv venv-xlrd
source ./venv-xlrd/bin/activate
pip install --upgrade pip
pip install xlrd
pip install xlwt
pip install xlsxwriter
source ./venv-xlrd/bin/activate

for F in $(ls *_Cards.xlsx)
do
	BASE=$(basename $F .xlsx)
	echo "Processing $F..."
	rm -f $BASE.db
	rm -f $BASE.sql
	if [ -f $BASE.xlsx ]; then 
		python c.py -i $BASE.xlsx -o $BASE.sql
		sqlite3 $BASE.db < $BASE.sql
		cp -p $BASE.db ../../tools/85b-swing-gui/
	else
		echo "$BASE.xlsx is missing"
	fi
done

exit 0

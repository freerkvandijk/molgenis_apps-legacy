#
# =====================================================
# $Id$
# $URL$
# $LastChangedDate$
# $LastChangedRevision$
# $LastChangedBy$
# =====================================================
#

#MOLGENIS walltime=10:00:00 nodes=1 cores=1 mem=10 clusterQueue=cluster
#INPUTS 
#OUTPUTS
#EXEC
#FOREACH flowcell, lane

<#include "helpers.ftl"/>

# create directories to put demultiplexed files in
<#list projectrawdatadir as thisoutputdir>
mkdir -p ${thisoutputdir}
</#list>
mkdir -p ${demultiplexinfodir}

# do the work
${demultiplexscript} \
--bc '${csv(barcode)}' \
--mismatches 1 \
--left  ${leftinputfile} \
--right ${rightinputfile} \
--check \
--out '${csv(outputleftright)}' \
--log ${logfile} \
--discardleft  ${discardleft} \
--discardright ${discardright}
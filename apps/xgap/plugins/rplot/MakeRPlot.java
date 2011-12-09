package plugins.rplot;

import java.io.File;
import java.util.TreeMap;

import matrix.DataMatrixInstance;

import org.molgenis.data.Data;
import org.molgenis.util.RScript;
import org.molgenis.util.RScriptException;

import plugins.matrix.manager.MatrixManagerModel;
import plugins.qtlfinder.QtlPlotDataPoint;

public class MakeRPlot
{
	public static File plot(Data data, DataMatrixInstance instance, String rowName, String colName, String action, String type, int width, int height) throws Exception
	{
		String rowType = data.getTargetType(); //shorthand
		String colType = data.getFeatureType(); //shorthand
		//Data data = model.getSelectedData(); //shorthand
		
		Object[] plotThis = null;
		PlotParameters params = new PlotParameters();
		
		if (action.endsWith("row"))
		{
			if (data.getValueType().equals("Text"))
			{
				params.setTitle(rowType + " " + rowName);
				params.setxLabel("Type of " + colType);
				params.setyLabel("# of " + colType);
			}else if (data.getValueType().equals("Decimal"))
			{
				params.setTitle(rowType + " " + rowName);
				params.setxLabel(colType);
				params.setyLabel(rowType + " value");
			}
			plotThis = instance.getRow(rowName);
		}
		
		else if(action.endsWith("col"))
		{
			if (data.getValueType().equals("Text"))
			{
				params.setTitle(colType + " " + colName);
				params.setxLabel("Type of " + rowType);
				params.setyLabel("# of " + rowType);

			}
			else if (data.getValueType().equals("Decimal"))
			{
				params.setTitle(colType + " " + colName);
				params.setxLabel(rowType);
				params.setyLabel(colType + " value");
			}
			plotThis = instance.getCol(colName);
		}
		else if(action.endsWith("heatmap"))
		{
			params.setTitle(instance.getData().getName());
			params.setxLabel("");
			params.setyLabel("");
		}
		else
		{
			throw new Exception("unrecognized action: " + action);
		}
		

			File tmpImg = new File(System.getProperty("java.io.tmpdir") + File.separator + "rplot"
					+ System.nanoTime() + ".png");
			params.setType(type);
			params.setWidth(width);
			params.setHeight(height);

			if(action.endsWith("col") || action.endsWith("row"))
			{
				if (type.equals("boxplot"))
				{
					params.setFunction("boxplot");
				}
				else
				{
					params.setFunction("plot");
				}

				new ScriptInstance(plotThis, tmpImg, params);
			}
			else if(action.endsWith("heatmap"))
			{
				new HeatmapScriptInstance(instance, tmpImg, params);
			}
			
			return tmpImg;
		
	}
	
	public static File qtlPlot(String plotName, TreeMap<Long, QtlPlotDataPoint> data, long genePos, int width, int height) throws RScriptException
	{
		double[] lodscores = new double[data.size()];
		long[] bplocs = new long[data.size()];
		String[] chromosomes = new String[data.size()];
		
		int index = 0;
		for(Long key : data.keySet())
		{
			lodscores[index] = data.get(key).getLodScore();
			bplocs[index] = data.get(key).getBpLoc();
			chromosomes[index] = data.get(key).getChromosome();
			index++;
		}
		return qtlPlot(plotName, lodscores, bplocs, chromosomes, genePos, width, height);
	}

	//all inputs must be sorted and of equal length!!
	//create QTL plot scaling by incrementing basepair position
	//give markers colours based on their chromosome
	//no missing values allowed!
	public static File qtlPlot(String plotName, double[] lodscores, long[] bplocs, String[] chromosomes, long genePos, int width, int height) throws RScriptException
	{
		File tmpImg = new File(System.getProperty("java.io.tmpdir") + File.separator + "rplot" + System.nanoTime() + ".png");
		
		RScript script = new RScript();
		RScript.R_COMMAND = "R CMD BATCH --vanilla --slave";
		script.append("imagefile <- \"" + tmpImg.getAbsolutePath().replace("\\", "/") + "\";");

		script.append("dataVector <- rep(0," + lodscores.length + ");");
		script.append("locs <- rep(0," + lodscores.length + ");");
		script.append("chrs <- rep(0," + lodscores.length + ");");

		for (int i = 0; i < lodscores.length; i++)
		{
			script.append("dataVector[" + (i + 1) + "] <- " + lodscores[i]);
			script.append("locs[" + (i + 1) + "] <- " + bplocs[i]);
			script.append("chrs[" + (i + 1) + "] <- \"" + chromosomes[i] + "\"");
		}
		script.append("chrs <- as.numeric(as.factor(chrs))+1");
		script.append("pos <- " + genePos);
		script.append("png(imagefile, width = " + width + ", height = " + height + ")");
		
		//start plotting: black line
		script.append("plot(y=dataVector,x=locs,col=\"black\",main=\"" + plotName + "\",xlab=\""
				+ "Basepair position" + "\",ylab=\"" + "LOD score" + "\",type=\"" + "l" + "\",pch=20,cex=2,lwd=2)");
		
		//now add coloured balls
		script.append("points(y=dataVector,x=locs,col=chrs,type=\"" + "p" + "\",pch=20,cex=2)");
		
		//now add vertical coloured lines
		script.append("points(y=dataVector,x=locs,col=chrs,type=\"" + "h" + "\",lwd=2)");
		
		//add transcript positions
		script.append("axis(1,pos,\"Transcript\",line=1)");
		script.append("abline(v=pos,lty=2,col='black')");
		
		//print to file
		script.append("dev.off()");
		script.execute();
		
		return tmpImg;
	}
	
	public static void main(String []args) throws RScriptException
	{
		File res = qtlPlot("henkie", new double[]{3,4,3,6,7,2,4,6}, new long[]{1,2,4,30,8,15,20,5}, new String[]{"I", "I", "I", "IV", "II", "III", "IV", "II"},25, 800, 600);
		System.out.println("RES @ " + res);
	}
}

package gollorum.signpost.util.math;

import java.util.Collection;

public class MatrixD {
	
	protected double[][] values;
	protected int rows;
	protected int columns;

	public MatrixD(int i, int j){
		values = new double[i][j];
		rows = i;
		columns = j;
	}
	
	public MatrixD(double[][] values){
		this.values = values.clone();
		repair();
	}
	
	public MatrixD(Collection<? extends Collection<Double>> values){
		this.values = new double[values.size()][];
		int i=0;
		for(Collection<Double> now: values){
			this.columns = Math.max(columns, now.size());
			this.values[i] = new double[columns];
			int j=0;
			for(Double val: now){
				this.values[i][j] = val.doubleValue();
				j++;
			}
			i++;
		}
		repair();
	}

	public double get(int i, int j){
		checkBounds(i, j);
		return values[i][j];
	}
	
	public void set(double value, int i, int j){
		checkBounds(i, j);
		values[i][j] = value;
	}

	public void gaussAlgorithm(){
		int offset = 0;
		for(int i=0; i<columns; i++){
			if(offset>=rows){
				break;
			}
			sortColumnBy0(i, offset);
			if(!test0(values[offset][i])){
				divideRow(offset, values[offset][i]);
				for(int j=0; j<offset; j++){
					substractRows(j, offset, values[j][i]);
				}
				for(int j=offset+1; j<rows; j++){
					substractRows(j, offset, values[j][i]);
				}
				offset++;
			}else{
				values[offset][i] = 0;
			}
		}
	}
	
	public boolean check(){
		boolean ret = true;
		for(int i=0; i<rows; i++){
			ret = ret&&checkRow(i);
		}
		return ret;
	}
	
	private boolean checkRow(int row){
		int n0c = 0;
		for(int i=0; i<columns-1; i++){
			if(!test0(values[row][i])){
				n0c++;
			}else{
				values[row][i] = 0;
			}
		}
		return !(n0c>1||(n0c==0&&!test0(values[row][columns-1])));
	}
	
	private void substractRows(int targetRow, int otherRow, double factor){
		for(int i=0; i<columns; i++){
			values[targetRow][i]-=values[otherRow][i]*factor;
		}
	}
	
	private void divideRow(int row, double divident){
		for(int i=0; i<columns; i++){
			values[row][i]/=divident;
		}
	}

	private void sortColumnBy0(int col, int offset){
		int row = rows-1;
		for(int j=row; j>=offset; j--){
			if(test0(values[j][col])){
				swapRows(j, row--);
			}
		}
	}

	private void repair(){
		rows = values.length;
		for(double[] now: values){
			columns = Math.max(columns, now.length);
		}
		for(int i=0; i<rows; i++){
			if(values[i].length!=columns){
				double[] vals = new double[columns];
				for(int j=0; j<values[i].length; j++){
					vals[j] = values[i][j];
				}
				values[i] = vals;
			}
		}
	}
	
	private void checkBounds(int i, int j) {
		if(i<0 || j<0 || i>=rows ||j>=columns){
			throw new IndexOutOfBoundsException("Out of matrix bounds: "+i+"|"+j+" ("+rows+"x"+columns+")");
		}
	}

	private void swapRows(int i, int j){
		double[] org = values[i];
		values[i] = values[j];
		values[j] = org;
	}

	public void print(){
		for(double[] row: values){
			System.out.print("|");
			for(double x: row){
				System.out.print(x+" ");
			}
			System.out.print("|");
			System.out.println();
		}
	}
	
	private boolean test0(double var){
		return -0.00000000001<var&&var<0.00000000001;
	}

	//Unused
	
	private void addRows(int targetRow, int otherRow, double factor){
		for(int i=0; i<columns; i++){
			values[targetRow][i]+=values[otherRow][i]*factor;
		}
	}

	private void multiplyRow(int row, double factor){
		for(int i=0; i<columns; i++){
			values[row][i]*=factor;
		}
	}

	private void swapColumns(int i, int j){
		for(int k=0; k<rows; k++){
			double val = values[k][i];
			values[k][i] = values[k][j];
			values[k][j] = val;
		}
	}

}

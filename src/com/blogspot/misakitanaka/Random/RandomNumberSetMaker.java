package com.blogspot.misakitanaka.Random;

import javax.swing.*;

import java.awt.*;
import java.io.*;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class RandomNumberSetMaker implements ActionListener,
		PropertyChangeListener {
	JTextField digitText;
	JTextField amountText;
	JTextArea noteDisplay;
	JButton button;
	RandomNumberSet r;
	ProgressMonitor progressMonitor;

	public static void main (String[] args){
		new RandomNumberSetMaker().go();
	}

	public void go (){
		JFrame frame = new JFrame();
		JPanel panel = new JPanel();
		
		button = new JButton("Start");
		button.addActionListener(this);

		digitText = new JTextField(5);
		amountText = new JTextField(5);
		
		JLabel digitLabel = new JLabel("Digit");
		JLabel amountLabel = new JLabel("Amount");

		noteDisplay = new JTextArea(2,20);
		noteDisplay.setEditable(false);
		noteDisplay.setText("Please enter digit and amount of the random number set you want to make.");
		noteDisplay.setOpaque(false);
		
		panel.add(digitLabel);
		panel.add(digitText);
		panel.add(amountLabel);
		panel.add(amountText);
		panel.add(button);

		frame.getContentPane().add(BorderLayout.CENTER, panel);
		frame.getContentPane().add(BorderLayout.SOUTH, noteDisplay);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if("progress".equals(arg0.getPropertyName())){
			int progress = r.getProgress();
			progressMonitor.setProgress(progress);
		}
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		button.setEnabled(false);


		if (!checkDigitAndAmount(digitText.getText(), amountText.getText())){
			button.setEnabled(true);
			return;
		}

		int digit = Integer.parseInt(digitText.getText());
		int amount = Integer.parseInt(amountText.getText());
		
		r = new RandomNumberSet();
		r.setDigit(digit);
		r.setAmount(amount);
		
    	progressMonitor = new ProgressMonitor(null, "Making random number set...","", 0, 100);
    	progressMonitor.setProgress(0);		
		
		r.addPropertyChangeListener(this);
		r.execute();
	}

	private boolean checkDigitAndAmount(String digit, String amount){
		
		if (!isInteger(digit) || !isInteger(amount)){
			String note = "Digit and amount should be number.";
			noteDisplay.setText(note);
			System.out.println(note);
			return false;			
		}
		
		int iDigit = Integer.parseInt(digit);
		int iAmount = Integer.parseInt(amount);
		
		if (iDigit < 8 || iDigit > 16){
			String note = "Digit should be between 8 and 16.";
			noteDisplay.setText(note);
			System.out.println(note);
			return false;
		}
		if (iAmount > 60000){
			String note = "Amount should be under 60000.";
			noteDisplay.setText(note);
			System.out.println(note);
			return false;
		}
		double limit = Math.pow(10, iDigit - 5);
		if (limit < iAmount){
			String note = "If digit is " + iDigit + ", amount should be under " + (long) limit + ".";
			noteDisplay.setText(note);
			System.out.println(note);
			return false;
		}
		return true;
	}
	
	private boolean isInteger( String input ){
	//From http://www.coderanch.com/t/401142/java/java/check-if-String-value	
	   try {  
	      Integer.parseInt( input );  
	      return true;  
	   }catch( Exception e){  
	      return false;  
	   }  
	}
	
	class RandomNumberSet extends SwingWorker<Void, Void> {
		HashSet<String> numberSet ;
		
		int digit;
		int amount;

		public void setDigit(int d){
			digit = d;
		}
		public void setAmount(int a){
			amount = a;
		}
		@Override
		protected Void doInBackground() throws Exception {

			System.out.println("Start! digit: " + digit + ", amount: " + amount);
			
			String regex = "123|234|345|456|567|678|789|890|098|987|876|765|654|543|432|321|210|(\\d)\\d*\\1\\d*\\1|(\\d+)\\d{0,2}\\2";
			Pattern p = Pattern.compile(regex);
			
			long draftCounter = 0;
			long notMuchCounter = 0;
			long notMuchToSetCounter = 0;
			long min = (long) Math.pow(10.0, digit - 1);
			long max = (long) Math.pow(10.0, digit)-1; 

			long number;
			numberSet = new HashSet<String>();

			draftCounter = 0;
			while (numberSet.size() < amount){
				draftCounter ++;
				number = (long) (Math.random() * (max - min + 1)) + min;
				String s =  new Long(number).toString();
				Matcher m = p.matcher(s);

				if (!m.find()){					
					if (!(checkNumberSet(s))){
						numberSet.add(Long.toString(number));
						setProgress((int)numberSet.size()* 90 / amount);
					}else{
						notMuchToSetCounter++;
					}
				}else{
					notMuchCounter ++;
					// System.out.println("not much 'like random number': " + s);
				}
				
				if (progressMonitor.isCanceled()){
					cancel(false);
					return null;
				}
			}
			System.out.println("Finished to generate. digit: " + digit + " amount: " + amount + " draft: " + draftCounter + " not much: " + notMuchCounter + " not much to set: " + notMuchToSetCounter );
			progressMonitor.setNote("Putting file 'ran.txt'");
			String s = getNumbers();
			progressMonitor.setProgress(95);
			putFile(s);
			progressMonitor.setProgress(progressMonitor.getMaximum());
			return null;
		}
		
		@Override
		public void done() {
			progressMonitor.close();
			if (isCancelled()){
				JOptionPane.showMessageDialog(null, "Canceled...");
				System.out.println("Canceled...");
			}else{		
				JOptionPane.showMessageDialog(null, "Finished!");
				System.out.println("Finished!");
			}
			button.setEnabled(true);			
		}
		
		private boolean checkNumberSet(String s){
			String regex = getPattern(s);
			Pattern p = Pattern.compile(regex);
			boolean result = false;
			for (String Element:numberSet){
				Matcher m = p.matcher(Element);
				result = m.find();
				if (result){
					System.out.println("The number like " + s + " already exists in number set :" + Element);
					break;
				}
			}
			return result;
		}

		private String getPattern(String s){
			String pattern = "";
			for (int i = 0; i < s.length()-2 ; i ++){
				String s1 = s.substring(0,i);
				String s2 = s.substring(i+1,s.length());
				String s3 = s1 + "\\d" + s2;
				pattern = pattern + "|" + s3;
			}
			pattern = pattern + "|" + s.substring(0, s.length() -2) + "\\d{2}";
			pattern = pattern.substring(1, pattern.length()); 
			return pattern;
		}

		private String getNumbers(){
			StringBuilder s = new StringBuilder();
			for (String element: numberSet){
				s.append(element + "\n");
			}
			return s.toString();
		} 
		
		private void putFile(String s){
			try{
				File outFile = new File("ran.txt");
				BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
				writer.write(s);
				writer.close();
			}catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	
}

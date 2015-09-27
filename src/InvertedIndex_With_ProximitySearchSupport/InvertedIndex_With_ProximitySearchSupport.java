package InvertedIndex_With_ProximitySearchSupport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.management.ImmutableDescriptor;

public class InvertedIndex_With_ProximitySearchSupport {


	@SuppressWarnings("rawtypes")
	public static void main(String[] args) 
	{

//		String FILE_PATH = "D:\\Eclipse Workspace\\IRassg1\\TestFiles\\file1.txt";
		String FILE_PATH = "D:\\Eclipse Workspace\\IRassg2\\testfiles\\r_and_j.txt";
//		String QUERY = "( schizophrenia /2 drug )";
		String QUERY = "( to /2 romeo )";
//		System.out.print("Enter absolute file path:");
//		FILE_PATH = System.console().readLine();
//		
//		System.out.print("Enter query [leave space before and after the parenthesis]:");
//		QUERY = System.console().readLine();
		
		
		//read file path
		File file = new File (FILE_PATH);

		List<List<String>> postings = new ArrayList<>();	// tokens X docID


		/*
		 * STEP 1: GENERATE POSTINGS
		 */

		//read the file line by line and process docs
		//Each line is a new doc and the starting word in a line is it's  docID
		try(BufferedReader br = new BufferedReader(new FileReader(file))) 
		{

			int docID =0;

			//for each line
			for(String line; (line = br.readLine()) != null;)
			{
				
				//increment docID
				docID++;
				
				// tokenize the line
				StringTokenizer tokens = new StringTokenizer(line);

				// skip the first token 
				tokens.nextToken();

				//keep track of position of tokens
				int position = 1;
				
				// read the remaining tokens and assign the docid
				while(tokens.hasMoreTokens())
				{					
					postings.add( Arrays.asList( tokens.nextToken(), Integer.toString(docID), Integer.toString(position) ) );
					position ++;
				}

			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		
//		System.out.print("");

		/*
		 * STEP 2: SORT THE POSTINGS
		 */

		sortListOnTerms(postings);
		
//		System.out.print("");
		
		/*
		 *  STEP 3: CREATE POSTING LISTS + MERGE DUPLICATES
		 */

		HashMap<String, TreeMap<Integer, List>> postingLists = createPostingLists(postings);

		/*
		 * STEP 4: EVALUATE THE PROXIMITY SEARCH QUERY
		 */
	
		List<List<Integer>> result = proximityQueryEval(QUERY, postingLists);
		
		
		/*
		 * STEP 5: PRINT THE RESULT
		 */
		
		return;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static HashMap<String, TreeMap<Integer, List>> createPostingLists(List<List<String>> postings) {
		
		/* 			 HashMap structure:
		 * 
		 * term -> 	 doc_id   ->   doc_id ->  .	. .	.
		 * 				|			|
		 * 				V			V
		 * 		 	positionList positionList
		*/
		HashMap<String, TreeMap<Integer, List>> postingLists = new HashMap<String, TreeMap<Integer, List>>();

		Iterator<List<String>> iterator = postings.iterator();
		
		//keep track of prev term
		String prevTerm = "";
		
		//keep track of prev docID
		String prevDocID = "";

		//for every entry in posting table
		while(iterator.hasNext())
		{
			//get the next entry: [term, docID, position] in the posting table
			List<String> list = iterator.next();

			//keep track of current term
			String currentTerm = list.get(0);
									
			//keep track of current docID
			String currentDocID = list.get(1);
			
			//case 1: new term
			if(!prevTerm.equals(currentTerm))
			{
				//create a new hashmap
				TreeMap<Integer, List> docID_posList = new TreeMap<>();				
				
				//create a new positionList and add the position to it
				List<Integer> temp_list = new ArrayList();
				temp_list.add(Integer.parseInt(list.get(2)));
				
				//add the docID as a key and positionList as a value
				docID_posList.put(Integer.parseInt(currentDocID), temp_list);			//NOTE: using ArrayList.asList() will cause problems below

				//add the current term as key and created hash map as value, to the main hashmap
				postingLists.put(currentTerm, docID_posList);
				
				prevTerm = currentTerm;
				prevDocID = currentDocID;

			}
			
			//case 2: old term
			else
			{
				//if the docID of the entry is same as the docID last added
				if(prevDocID.equals(currentDocID))
				{
					//add the position to the docID's position list
					postingLists.get(currentTerm).get(Integer.parseInt(currentDocID)).add(Integer.parseInt(list.get(2)));
				}
				
				//if the docID differs from the last time
				else
				{	
					//create a new positionList and add the position to it
					List<Integer> temp_list = new ArrayList();
					temp_list.add(Integer.parseInt(list.get(2)));
					
					//add the new docID and position as a new entry
					postingLists.get(currentTerm).put(Integer.parseInt(currentDocID), temp_list);
					prevDocID = currentDocID;
				}
				
				
			}
		}

		
		return postingLists;
		
	}

	@SuppressWarnings("all")
	private static List<List<Integer>> proximityQueryEval(String QUERY, HashMap<String, TreeMap<Integer, List>> postingLists) {

		/*
		 *  Query preprocessing
		 */
		
		StringTokenizer queryTokens = new StringTokenizer(QUERY);
		
		queryTokens.nextToken();	//skip over first parenthesis
		
		String word1 = queryTokens.nextToken();
		
		int proximity = Integer.parseInt(queryTokens.nextToken().split("\\/")[1]);
		
		String word2 = queryTokens.nextToken();
		
		/*
		 *  Initialization
		 */
		
		TreeMap<Integer, List> term1_docIDs_and_positionLists =  postingLists.get(word1);
		TreeMap<Integer, List> term2_docIDs_and_positionLists =  postingLists.get(word2);
		
		List<Integer> term1_docID_list = new ArrayList<>(); 
		term1_docID_list.addAll(term1_docIDs_and_positionLists.keySet());

		List<Integer> term2_docID_list = new ArrayList<>(); 
		term2_docID_list.addAll(term2_docIDs_and_positionLists.keySet());

		Integer postingListSize_1 = term1_docID_list.size();
		Integer postingListSize_2 = term2_docID_list.size();
		
		List<List<Integer>> result = new ArrayList<>();		
				
		int index1 = 0;
		int index2 = 0;
		
		while(index1 < postingListSize_1 && index2 < postingListSize_2)
		{
			if(term1_docID_list.get(index1).equals(term2_docID_list.get(index2)))
			{
				List<Integer> list = new ArrayList<>();
				
				List<Integer> positionList1 = term1_docIDs_and_positionLists.get(term1_docID_list.get(index1));				
				List<Integer> positionList2 = term2_docIDs_and_positionLists.get(term2_docID_list.get(index2));
				
				if(index1 == 3)
					System.out.println("");
				
				int positionListIndex1 = 0;
				int positionListIndex2 = 0;
				
				while(positionListIndex1 < positionList1.size())
				{
					while(positionListIndex2 < positionList2.size())
					{
						if(positionList2.get(positionListIndex2) - positionList1.get(positionListIndex1) <= proximity
							&& positionList2.get(positionListIndex2) - positionList1.get(positionListIndex1) >0)
						{
							list.add(positionList2.get(positionListIndex2));
						}
						
						else if(positionList2.get(positionListIndex2) > positionList1.get(positionListIndex1))
							break;
						
						positionListIndex2++;
						
						while(!list.isEmpty() && Math.abs(list.get(0) - positionList1.get(positionListIndex1)) > proximity )
							list.remove(0);
						
						for(int ps : list)
							result.add(Arrays.asList( term1_docID_list.get(index1), positionList1.get(positionListIndex1), ps));							
												
					}
					
					positionListIndex1++;
				}
				
				index1++;
				index2++;				
			}
			
			else if(term1_docID_list.get(index1) < term2_docID_list.get(index2))
				index1++;
			else
				index2++;
		}
		
		return result;
	}


	private static void sortListOnTerms(List<List<String>> postings) {

		Collections.sort(postings, new Comparator<List<String>>() 
		{    
			@Override
			public int compare(List<String> a1, List<String> a2) 
			{
				return a1.get(0).compareTo(a2.get(0));
			}               
		});
	}

}

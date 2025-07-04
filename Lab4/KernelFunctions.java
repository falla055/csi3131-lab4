import cern.jet.random.engine.RandomEngine;
import EvSchedSimul.*;

public class KernelFunctions
{
	//******************************************************************
	//                 Methods for supporting page replacement
	//******************************************************************

	// the following method calls a page replacement method once
	// all allocated frames have been used up
	// DO NOT CHANGE this method
	public static void pageReplacement(int vpage, Process prc, Kernel krn, double clock)
	{
	   if(prc.pageTable[vpage].valid) return;   // no need to replace

	   if(!prc.areAllocatedFramesFull()) // room to get frames in allocated list
 	       addPageFrame(vpage, prc, krn);
	   else
	       pageReplAlgorithm(vpage, prc, krn, clock);
	}

	// This method will all a page frame to the list of allocated 
	// frames for the process and load it with the virtual page
	// DO NOT CHANGE this method
	public static void addPageFrame(int vpage, Process prc, Kernel krn)
	{
	     int [] fl;  // new frame list
	     int freeFrame;  // a frame from the free list
	     int i;
	     // Get a free frame and update the allocated frame list
	     freeFrame = krn.getNextFreeFrame();  // gets next free frame
	     if(freeFrame == -1)  // list must be empty - print error message and return
	     {
		System.out.println("Could not get a free frame");
		return;
	     }
	     if(prc.allocatedFrames == null) // No frames allocated yet
	     {
		prc.allocatedFrames = new int[1];
		prc.allocatedFrames[0] = freeFrame;
	     }
	     else // some allocated but can get more
	     {
	        fl = new int[prc.allocatedFrames.length+1];
	        for(i=0 ; i<prc.allocatedFrames.length ; i++) fl[i] = prc.allocatedFrames[i];
	        fl[i] = freeFrame; // adds free frame to the free list
	        prc.allocatedFrames = fl; // keep new list
	     }
	     // update Page Table
	     prc.pageTable[vpage].frameNum = freeFrame;
	     prc.pageTable[vpage].valid = true;
	}

	// Calls to Replacement algorithm
	public static void pageReplAlgorithm(int vpage, Process prc, Kernel krn, double clock)
	{
	     boolean doingCount = false;
	     switch(krn.pagingAlgorithm)
	     {
		case FIFO: pageReplAlgorithmFIFO(vpage, prc); break;
		case LRU: pageReplAlgorithmLRU(vpage, prc); break;
		case CLOCK: pageReplAlgorithmCLOCK(vpage, prc); break;
		case COUNT: pageReplAlgorithmCOUNT(vpage, prc); doingCount=true; break;
		case WORKING_SET: pageReplAlgorithmWorkingSet(vpage, prc, krn, clock); break;
		case RANDOM: pageReplAlgorithmRandom(vpage, prc, krn, krn.rand); break;
	     }
	}
	
	//--------------------------------------------------------------
	//  The following methods need modification to implement
	//  the three page replacement algorithms
	//  ------------------------------------------------------------
	// The following method is called each time an access to memory
	// is made (including after a page fault). It will allow you
	// to update the page table entries for supporting various
	// page replacement algorithms.
	public static void doneMemAccess(int vpage, Process prc, double clock)
	{
        // For LRU: update timestamp
        prc.pageTable[vpage].tmStamp = clock;
        // For CLOCK: set used bit
        prc.pageTable[vpage].used = true;
        // For COUNT: increment count
        prc.pageTable[vpage].count++;
    }

    // FIFO page Replacement algorithm
    public static void pageReplAlgorithmFIFO(int vpage, Process prc)
    {
       int vPageReplaced;  // Page to be replaced
       int frame;	// frame to receive new page
       // Find page to be replaced
       frame = prc.allocatedFrames[prc.framePtr];   // get next available frame
       vPageReplaced = findvPage(prc.pageTable,frame);     // find current page using it (i.e written to disk)
       prc.pageTable[vPageReplaced].valid = false;  // Old page is replaced.
       prc.pageTable[vpage].frameNum = frame;   // load page into the frame and update table
       prc.pageTable[vpage].valid = true;             // make the page valid
       prc.framePtr = (prc.framePtr+1) % prc.allocatedFrames.length;  // point to next frame in list
    }

    // CLOCK page Replacement algorithm
    public static void pageReplAlgorithmCLOCK(int vpage, Process prc)
    {
        int n = prc.allocatedFrames.length;
        while (true) {
            int frame = prc.allocatedFrames[prc.framePtr];
            int candidate = findvPage(prc.pageTable, frame);
            if (!prc.pageTable[candidate].used) {
                // Replace this page
                prc.pageTable[candidate].valid = false;
                prc.pageTable[vpage].frameNum = frame;
                prc.pageTable[vpage].valid = true;
                prc.pageTable[vpage].used = true; // Set used bit for new page
                prc.framePtr = (prc.framePtr + 1) % n;
                break;
            } else {
                // Give second chance
                prc.pageTable[candidate].used = false;
                prc.framePtr = (prc.framePtr + 1) % n;
            }
        }
    }

    // LRU page Replacement algorithm
    public static void pageReplAlgorithmLRU(int vpage, Process prc)
    {
        int n = prc.allocatedFrames.length;
        double minTime = Double.MAX_VALUE;
        int lruFrameIdx = -1;
        int lruPage = -1;

        // Find the least recently used page among allocated frames
        for (int i = 0; i < n; i++) {
            int frame = prc.allocatedFrames[i];
            int page = findvPage(prc.pageTable, frame);
            double t = prc.pageTable[page].tmStamp;
            if (t < minTime) {
                minTime = t;
                lruFrameIdx = i;
                lruPage = page;
            }
        }
        // Replace the LRU page
        int frame = prc.allocatedFrames[lruFrameIdx];
        prc.pageTable[lruPage].valid = false; // Invalidate last page
        prc.pageTable[vpage].frameNum = frame; // map new page to this frame
        prc.pageTable[vpage].valid = true; // validate new page
        prc.pageTable[vpage].tmStamp = System.currentTimeMillis(); // will be updated by doneMemAccess
    }

    // COUNT page Replacement algorithm
    public static void pageReplAlgorithmCOUNT(int vpage, Process prc)
    {
    }

    // WORKING_SET page Replacement algorithm
    public static void pageReplAlgorithmWorkingSet(int vpage, Process prc, Kernel krn, double clock)
    {
        double window = 0.0; // Working set window size 
        double oldest = Double.MAX_VALUE;
        int victim = -1;
        int n = prc.allocatedFrames.length;

        for (int i = 0; i < n; i++) {
            int frame = prc.allocatedFrames[i];
            int page = findvPage(prc.pageTable, frame);
            double lastAccess = prc.pageTable[page].tmStamp;
            // If not in working set (not used in window), prefer as victim
            if (clock - lastAccess > window) {
                victim = page;
                break;
            }
            // Otherwise, track oldest access
            else if (lastAccess < oldest) {
                oldest = lastAccess;
                victim = page;
            }
        }
        // Replace victim
        int frame = prc.pageTable[victim].frameNum;
        prc.pageTable[victim].valid = false; // Invalidate old page
        prc.pageTable[vpage].frameNum = frame; // map new page to this frame
        prc.pageTable[vpage].valid = true; 
        prc.pageTable[vpage].tmStamp = clock; // set last access to now
    }

	public static void pageReplAlgorithmRandom(int vpage, Process prc, Kernel krn, RandomEngine rand)
{
    int n = prc.allocatedFrames.length;
    int victimIdx = Math.abs(rand.nextInt()) % n;  // pick random index in allocated frames

    int frame = prc.allocatedFrames[victimIdx];
    int victimPage = findvPage(prc.pageTable, frame);

    // Replace victim
    prc.pageTable[victimPage].valid = false;
    prc.pageTable[vpage].frameNum = frame;
    prc.pageTable[vpage].valid = true;
    prc.pageTable[vpage].used = true; // or set to false, depending on your policy
}

	// finds the virtual page loaded in the specified frame fr
	public static int findvPage(PgTblEntry [] ptbl, int fr)
	{
	   int i;
	   for(i=0 ; i<ptbl.length ; i++)
	   {
	       if(ptbl[i].valid)
	       {
	          if(ptbl[i].frameNum == fr)
	          {
		      return(i);
	          }
	       }
	   }
	   System.out.println("Could not find frame number in Page Table "+fr);
	   return(-1);
	}

	// *******************************************
	// The following method is provided for debugging purposes
	// Call it to display the various data structures defined
	// for the process so that you may examine the effect
	// of your page replacement algorithm on the state of the 
	// process.
        // Method for displaying the state of a process
	// *******************************************
	public static void logProcessState(Process prc)
	{
	   int i;

	   System.out.println("--------------Process "+prc.pid+"----------------");
	   System.out.println("Virtual pages: Total: "+prc.numPages+
			      " Code pages: "+prc.numCodePages+
			      " Data pages: "+prc.numDataPages+
			      " Stack pages: "+prc.numStackPages+
			      " Heap pages: "+prc.numHeapPages);
	   System.out.println("Simulation data: numAccesses left in cycle: "+prc.numMemAccess+
			      " Num to next change in working set: "+prc.numMA2ChangeWS);
           System.out.println("Working set is :");
           for(i=0 ; i<prc.workingSet.length; i++)
           {
              System.out.print(" "+prc.workingSet[i]);
           }
           System.out.println();
	   // page Table
	   System.out.println("Page Table");
	   if(prc.pageTable != null)
	   {
	      for(i=0 ; i<prc.pageTable.length ; i++)
	      {
		 if(prc.pageTable[i].valid) // its valid printout the data
		 {
	           System.out.println("   Page "+i+"(valid): "+
				      " Frame "+prc.pageTable[i].frameNum+
				      " Used "+prc.pageTable[i].used+
				      " count "+prc.pageTable[i].count+
				      " Time Stamp "+prc.pageTable[i].tmStamp);
		 }
		 else System.out.println("   Page "+i+" is invalid (i.e not loaded)");
	      }
	   }
	   // allocated frames
	   System.out.println("Allocated frames (max is "+prc.numAllocatedFrames+")"+
			      " (frame pointer is "+prc.framePtr+")");
	   if(prc.allocatedFrames != null)
	   {
	      for(i=0 ; i<prc.allocatedFrames.length ; i++)
 	           System.out.print(" "+prc.allocatedFrames[i]);
	   }
	   System.out.println();
           System.out.println("---------------------------------------------");
	}
}



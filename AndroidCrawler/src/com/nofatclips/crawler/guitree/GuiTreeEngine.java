package com.nofatclips.crawler.guitree;

import java.util.GregorianCalendar;

import javax.xml.parsers.ParserConfigurationException;

import com.nofatclips.androidtesting.guitree.GuiTree;
import com.nofatclips.crawler.Engine;
import com.nofatclips.crawler.automation.Automation;
import com.nofatclips.crawler.automation.BasicRestarter;
import com.nofatclips.crawler.automation.SimpleTypeDetector;
import com.nofatclips.crawler.filters.FormFilter;
import com.nofatclips.crawler.filters.SimpleEventFilter;
import com.nofatclips.crawler.model.Filter;
import com.nofatclips.crawler.planning.SimplePlanner;
import com.nofatclips.crawler.planning.SimpleUser;
import com.nofatclips.crawler.planning.TraceDispatcher;
import com.nofatclips.crawler.storage.DiskPersistence;
import com.nofatclips.crawler.storage.StepDiskPersistence;
import com.nofatclips.crawler.strategy.*;

import static com.nofatclips.crawler.Resources.*;

public class GuiTreeEngine extends Engine {

	public GuiTreeEngine () {
		super ();
				
		setScheduler(new TraceDispatcher());
		
		this.theAutomation = new Automation();
		this.theRestarter = new BasicRestarter();
		this.theAutomation.setRestarter(theRestarter);
		setRobot (this.theAutomation);
		setExtractor (this.theAutomation);
		
		try {
			GuiTree.setValidation(false);
			this.guiAbstractor = new GuiTreeAbstractor();
			this.theGuiTree = this.guiAbstractor.getTheSession();
			GregorianCalendar c=new GregorianCalendar();
			theGuiTree.setDateTime(c.getTime().toString());
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setAbstractor(this.guiAbstractor);
		setSession (this.theGuiTree);

		SimplePlanner p = new SimplePlanner();

		Filter inputFilter = new FormFilter();
		p.setInputFilter (inputFilter);
		this.guiAbstractor.addFilter (inputFilter);

		Filter eventFilter = new SimpleEventFilter();
		p.setEventFilter (eventFilter);
		this.guiAbstractor.addFilter (eventFilter);
		this.guiAbstractor.setTypeDetector(new SimpleTypeDetector());
		
		this.user = new SimpleUser(this.guiAbstractor);
		p.setUser(user);
		p.setFormFiller(user);
		p.setAbstractor(this.guiAbstractor);
		setPlanner (p);
		
		StrategyFactory sf = new StrategyFactory(COMPARATOR, ADDITIONAL_CRITERIAS);
		sf.setDepth(TRACE_MAX_DEPTH);
		sf.setMaxTraces(MAX_NUM_TRACES);
		sf.setMaxSeconds(MAX_TIME_CRAWLING);
		sf.setCheckTransitions(CHECK_FOR_TRANSITION);
		setStrategy (sf.getStrategy());
		
//		if (useCustomStrategy ()) {
//			CustomStrategy s = new CustomStrategy(COMPARATOR, new NewActivityExplorer());
//			if (checkMaxTraces()) {
//				s.addTerminationCriteria(new MaxStepsTermination(MAX_NUM_TRACES));
//			}
//			if (checkForDepth()) {
//				s.addExplorationCriteria(new DepthExplorer(TRACE_MAX_DEPTH));
//			}
//			if (checkTransition()) {
//				s.addTransitionCriteria(new NewActivityTransitioner());				
//			}
//			setStrategy(s);
//		} else {
//			SimpleStrategy s = new SimpleStrategy (COMPARATOR);
//			if (checkMaxTraces()) {
//				s.addTerminationCriteria(new MaxStepsTermination(MAX_NUM_TRACES));
//			}
//			setStrategy(s);
//		}
		
		d = (stepPersistence())?new StepDiskPersistence (MAX_TRACES_IN_RAM):new DiskPersistence();
		d.setSession(this.theGuiTree);
		setPersistence (d);
		
	}
	
//	public boolean useCustomStrategy () {
//		return (checkTransition() || checkForDepth());
//	}
//	
//	public boolean checkForDepth() {
//		return (TRACE_MAX_DEPTH>0);
//	}
//	
//	public boolean checkMaxTraces() {
//		return (MAX_NUM_TRACES>0);
//	}
//	
//	public boolean checkTransition() {
//		return CHECK_FOR_TRANSITION;
//	}
	
	protected void setUp () {
		try {
			super.setUp();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		d.setContext(theAutomation.getActivity());
		theRestarter.setRestartPoint(theAutomation.getActivity());
		theGuiTree.setAppName(theAutomation.getAppName());
		theGuiTree.setSleepAfterEvent(SLEEP_AFTER_EVENT);
		theGuiTree.setSleepAfterRestart(SLEEP_AFTER_RESTART);
		theGuiTree.setClassName(CLASS_NAME);
		theGuiTree.setPackageName(PACKAGE_NAME);
	}
	
	public boolean stepPersistence () {
		return (MAX_TRACES_IN_RAM>0);
	}
	
	private Automation theAutomation;
	private GuiTreeAbstractor guiAbstractor;
	private SimpleUser user;
	private BasicRestarter theRestarter;
	private GuiTree theGuiTree;
	private DiskPersistence d;
	
}

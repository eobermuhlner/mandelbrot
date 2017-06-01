package ch.obermuhlner.mandelbrot.poi;

import java.math.BigDecimal;

public class StandardPointsOfInterest {

	public static PointOfInterest[] POINTS_OF_INTEREST = {
			new PointOfInterest(
					"Initial",
					new BigDecimal("0"),
					new BigDecimal("0"),
					0,
					14,
					20),
			new PointOfInterest(
					"Curved Swords",
					new BigDecimal("0.017919288259557892593847458731170858208746794667084869140010189701941671977885175491969822815680324832650288118287542225754354320968861805008056865043840568408674639001904242945377670838006845358428388295630356802224461622401647761424530072"),
					new BigDecimal("1.01176097531987061853463090956462940839175503684365364035585094020187264122252703179366559338808085425394834006826628793779676461877510495451535804537480898432580908583825399893338707858055917113599638919828012253992937320289924731178786342"),
					10.9, // until 190
					1,
					10),
			new PointOfInterest(
					"Snail Shell",
					new BigDecimal("1.749721929742338571328512183204793465117897644259904770681747353482121708665972660839800936317633296441980469665826685985285388"),
					new BigDecimal("-0.000029016647753686084545360932647113026525960648184743451989371745927172996759411354785502498950354939680922076169129062986700"),
					10.0, // until 76
					4,
					20),
			new PointOfInterest(
					"Chaotic Spirals",
					new BigDecimal("-0.2634547678695909194066896880514263352460531386654479061034372986938"),
					new BigDecimal("-0.002712500848707182758893848976049032980969922260445790785383665852890"),
					4.9, // until 19
					12,
					100),
			new PointOfInterest(
					"Hydra",
					new BigDecimal("-0.047296221989823492360680509805723234463608050671596296439830057098"),
					new BigDecimal("-0.66103581599868663245324926435494860068349798558883084891691182468"),
					10.0, // until 17
					7,
					50),
			new PointOfInterest(
					"Nautilus Swarm",
					new BigDecimal("0.74364388856444046004654780500276747265963221621866228900489806506969863376"),
					new BigDecimal("0.13182590425618977278061359608949824676900962922349361652463219715391690224"),
					9.8, // until 25
					1,
					10),
			new PointOfInterest(
					"Wheels of Fire",
					new BigDecimal("1.348297061455330911551814316377597957726489116406445444186894528900140472"),
					new BigDecimal("0.049008405244229690790778032639417178346556340335816072865655769089707512"),
					9.8, // until 23
					8,
					200),
			new PointOfInterest(
					"Endless Spirals",
					new BigDecimal("-0.327723250308055864649016619795523424768319176482519467731956421519920253447363086958917926666960824258120609388163651727300535678400149632928730434212900"),
					new BigDecimal("0.0371201060581503985963131260660658234700437982666285956535462383384890416047297529586437189049645110208865816021476356710788140673394527005494736015790"),
					9.0, // until 100
					30,
					20),
			new PointOfInterest(
					"Jelly Fish",
					new BigDecimal("1.62060149612909895334558638175468271327989345822610379886703941363672342"),
					new BigDecimal("0.00684632316877134945739460204744981575825547591137510013929931504873578"),
					10.0, // until 20
					3,
					30),
			new PointOfInterest(
					"Nested Spirals",
					new BigDecimal("-0.2634547678695909194010687726751533779835323018724011267240225630781064993853207559075347569585780106855530658121171758"),
					new BigDecimal("-0.0027125008487071827834443496304877516301258356448567941355077892010367321774680744357866562509376971321700375426804242"),
					8.0, // until 75
					6,
					40),
			new PointOfInterest(
					"Thorns",
					new BigDecimal("0.615688188277165136862977361674265969958593022307313876044710397223212241218305144722407409388125158236774855883651489995471305785441350335740253105778"),
					new BigDecimal("0.674900407359391397989165449336345186641209056492297641703764886106334430140801874852392546319746961769590518919533419668508561716801971179771345638618"),
					8.4, // until 100
					1,
					10),
			new PointOfInterest(
					"Deep Zoom 1",
					new BigDecimal("1.62874368462587295806106783882602507628127773651120323551549048375276649835155422126891744930000524026194460775214654577403746763682355851846408284526"),
					new BigDecimal("0.033215675354500494972839691478104380482125020700561059129514348172864294223410788230689033349273198394359445030319755598713727413106785213951017375984"),
					10.0, // until 100
					1,
					10),
			new PointOfInterest(
					"Deep Zoom 2",
					new BigDecimal("0.173972895149861696398245419815762611049708791210778917554168116389140805300330911839141960207434807394772877483117083809325451019282668525307165239006"),
					new BigDecimal("1.087345391589215514972565136866633051757799438102011261200069713282755564505200529623697527627089154768928727180456140151882187330986620179577468138506"),
					4.0, // until 100
					1,
					10),
			new PointOfInterest(
					"Classic Deep",
					new BigDecimal("1.740062382579339905220844167065825638296641720436171866879862418461182919644153056054840718339483225743450008259172138785492983677893366503417299549623738838303346465461290768441055486136870719850559269507357211790243666940134793753068611574745943820712885258222629105433648695946003865"),
					new BigDecimal("0.0281753397792110489924115211443195096875390767429906085704013095958801743240920186385400814658560553615695084486774077000669037710191665338060418999324320867147028768983704831316527873719459264592084600433150333362859318102017032958074799966721030307082150171994798478089798638258639934"),
					5.0,
					1,
					5),
			new PointOfInterest(
					"Close to the Tip",
					new BigDecimal("1.999774075531510062196466924922751584703084668836849819676693632045811372575383342685205522372465313107518959003812"),
					new BigDecimal("3.375402489828553121033532529759462615490836723815772824E-60"),
					3.0, // until 18
					1,
					5),
			new PointOfInterest(
					"Snowflakes",
					new BigDecimal("0.5626818719638919630176963442920970094501055623422178206065541722507806041463424"),
					new BigDecimal("0.6422559016143566572157169421153624880738140999727913157684393971646400742210028"),
					3.0, // until 30
					24,
					20),
	};

	public static PointOfInterest findPointOfInterest(String poiName) {
		for (PointOfInterest pointOfInterest : StandardPointsOfInterest.POINTS_OF_INTEREST) {
			if (pointOfInterest.name.equals(poiName)) {
				return pointOfInterest;
			}
		}
		
		return null;
	}
}

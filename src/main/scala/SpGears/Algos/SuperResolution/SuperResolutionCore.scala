package SpGears.Algos.SuperResolution

import spinal.lib._
import spinal.core._
import spinal.lib.bus.amba4.axilite._

case class SuperResolutionCore(config: IPConfig, axiLiteConfig: AxiLite4Config) extends Component {
  val ddrDataIn = slave Stream UInt(4 * config.dataW bits)
  val ipConfig  = slave(AxiLite4(axiLiteConfig))
//  val inpDone    = out Bool ()
  val inpDataOut = master Stream PixelData(config, allChannel = true)

  /* initiate DDRDataWrapper to get PixelData type Stream */
  val ddrDataWrapper = DDRDataWrapper(config)

  /* initiate dispatcher to transform the rgb channel data to interpolate component */
  val channelDispatcher = ChannelMasterTransformer(config)

  /* initiate the interpolate component compute core */
  val bChannelPart1Core = SuperResolutionPart1(config)
  val bChannelPart2Core = SuperResolutionPart2(config)
  val bChannelPart3Core = SuperResolutionPart3(config)

  val gChannelPart1Core = SuperResolutionPart1(config)
  val gChannelPart2Core = SuperResolutionPart2(config)
  val gChannelPart3Core = SuperResolutionPart3(config)

  val rChannelPart1Core = SuperResolutionPart1(config)
  val rChannelPart2Core = SuperResolutionPart2(config)
  val rChannelPart3Core = SuperResolutionPart3(config)

  /* initiate the combiner to transform the rgb channel data to single channel to ddr */
  val channelCombiner = ChannelSlaveTransformer(config)

  /* initiate the axiLite config component to set ip config */
  val inpConfig = InpConfig(config, axiLiteConfig)

  /* for ap_done signal */
  val rDone          = RegNext(rChannelPart1Core.io.inpDoneOut).init(False)
  val gDone          = RegNext(gChannelPart1Core.io.inpDoneOut).init(False)
  val bDone          = RegNext(bChannelPart1Core.io.inpDoneOut).init(False)
  val allChannelDone = rDone && gDone && bDone
  when(allChannelDone) {
    rDone.clear()
    gDone.clear()
    bDone.clear()
  }

  /* connect components */
//  inpDone := allChannelDone

  ddrDataIn               >> ddrDataWrapper.ddrIn
  ddrDataWrapper.bmpWidth := inpConfig.io.srcWidth
  ddrDataWrapper.inpDone  := allChannelDone

  ddrDataWrapper.pixelsOut           >> channelDispatcher.allPixelChannelIn
  channelDispatcher.rPixelChannelOut >> rChannelPart1Core.io.pixelsIn
  channelDispatcher.gPixelChannelOut >> gChannelPart1Core.io.pixelsIn
  channelDispatcher.bPixelChannelOut >> bChannelPart1Core.io.pixelsIn

  ipConfig            >> inpConfig.io.axiLiteSignal
  inpConfig.io.apDone := allChannelDone

  rChannelPart1Core.io.widthIn := inpConfig.io.srcWidth
  gChannelPart1Core.io.widthIn := inpConfig.io.srcWidth
  bChannelPart1Core.io.widthIn := inpConfig.io.srcWidth

  rChannelPart2Core.io.widthIn := inpConfig.io.srcWidth
  gChannelPart2Core.io.widthIn := inpConfig.io.srcWidth
  bChannelPart2Core.io.widthIn := inpConfig.io.srcWidth

  rChannelPart3Core.io.widthIn := inpConfig.io.srcWidth
  gChannelPart3Core.io.widthIn := inpConfig.io.srcWidth
  bChannelPart3Core.io.widthIn := inpConfig.io.srcWidth

  rChannelPart1Core.io.heightIn := inpConfig.io.srcHeight
  gChannelPart1Core.io.heightIn := inpConfig.io.srcHeight
  bChannelPart1Core.io.heightIn := inpConfig.io.srcHeight

  rChannelPart2Core.io.heightIn := inpConfig.io.srcHeight
  gChannelPart2Core.io.heightIn := inpConfig.io.srcHeight
  bChannelPart2Core.io.heightIn := inpConfig.io.srcHeight

  rChannelPart3Core.io.heightIn := inpConfig.io.srcHeight
  gChannelPart3Core.io.heightIn := inpConfig.io.srcHeight
  bChannelPart3Core.io.heightIn := inpConfig.io.srcHeight

  rChannelPart1Core.io.thresholdIn := inpConfig.io.threshold
  gChannelPart1Core.io.thresholdIn := inpConfig.io.threshold
  bChannelPart1Core.io.thresholdIn := inpConfig.io.threshold

  rChannelPart2Core.io.thresholdIn := inpConfig.io.threshold
  gChannelPart2Core.io.thresholdIn := inpConfig.io.threshold
  bChannelPart2Core.io.thresholdIn := inpConfig.io.threshold

  rChannelPart3Core.io.thresholdIn := inpConfig.io.threshold
  gChannelPart3Core.io.thresholdIn := inpConfig.io.threshold
  bChannelPart3Core.io.thresholdIn := inpConfig.io.threshold

  rChannelPart1Core.io.startIn := inpConfig.io.apStart
  gChannelPart1Core.io.startIn := inpConfig.io.apStart
  bChannelPart1Core.io.startIn := inpConfig.io.apStart

  rChannelPart1Core.io.pixelsOut      >> rChannelPart2Core.io.pixelsIn
  rChannelPart1Core.io.inpTwoDoneIn   := rChannelPart2Core.io.inpTwoDoneOut
  rChannelPart1Core.io.inpThreeDoneIn := rChannelPart3Core.io.inpThreeDoneOut

  rChannelPart2Core.io.startIn        := rChannelPart1Core.io.startOut
  rChannelPart2Core.io.pixelsOut      >> rChannelPart3Core.io.pixelsIn
  rChannelPart2Core.io.inpThreeDoneIn := rChannelPart3Core.io.inpThreeDoneOut

  rChannelPart3Core.io.startIn   := rChannelPart2Core.io.startOut
  rChannelPart3Core.io.pixelsOut >> channelCombiner.rPixelChannelIn

  gChannelPart1Core.io.pixelsOut      >> gChannelPart2Core.io.pixelsIn
  gChannelPart1Core.io.inpTwoDoneIn   := gChannelPart2Core.io.inpTwoDoneOut
  gChannelPart1Core.io.inpThreeDoneIn := gChannelPart3Core.io.inpThreeDoneOut

  gChannelPart2Core.io.startIn        := gChannelPart1Core.io.startOut
  gChannelPart2Core.io.pixelsOut      >> gChannelPart3Core.io.pixelsIn
  gChannelPart2Core.io.inpThreeDoneIn := gChannelPart3Core.io.inpThreeDoneOut

  gChannelPart3Core.io.startIn   := gChannelPart2Core.io.startOut
  gChannelPart3Core.io.pixelsOut >> channelCombiner.gPixelChannelIn

  bChannelPart1Core.io.pixelsOut      >> bChannelPart2Core.io.pixelsIn
  bChannelPart1Core.io.inpTwoDoneIn   := bChannelPart2Core.io.inpTwoDoneOut
  bChannelPart1Core.io.inpThreeDoneIn := bChannelPart3Core.io.inpThreeDoneOut

  bChannelPart2Core.io.startIn        := bChannelPart1Core.io.startOut
  bChannelPart2Core.io.pixelsOut      >> bChannelPart3Core.io.pixelsIn
  bChannelPart2Core.io.inpThreeDoneIn := bChannelPart3Core.io.inpThreeDoneOut

  bChannelPart3Core.io.startIn   := bChannelPart2Core.io.startOut
  bChannelPart3Core.io.pixelsOut >> channelCombiner.bPixelChannelIn

  channelCombiner.allPixelChannelOut >> inpDataOut

}

object GenIPCode extends App {
  SpinalConfig(
    mode = Verilog,
    defaultConfigForClockDomains = ClockDomainConfig(
      clockEdge        = RISING,
      resetKind        = ASYNC,
      resetActiveLevel = LOW
    ),
    targetDirectory = "./IPCode"
  ).generateVerilog(SuperResolutionCore(IPConfig(), axiLiteConfig = AxiLite4Config(dataWidth = 32, addressWidth = 32)))
}

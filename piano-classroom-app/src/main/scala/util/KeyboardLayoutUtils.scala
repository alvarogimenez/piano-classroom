package util

import java.awt.Color
import java.awt.image.BufferedImage

import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.ThresholdFilter
import util.MusicNote.MusicNote

import scala.collection.mutable.ArrayBuffer


object KeyboardLayoutUtils {
  case class PatternI(x: Int, i: Double)
  case class KeyHBoundingBox(key: KeyboardNote, left: Int, right: Int)
  case class KeyBoundingBox(key: KeyboardNote, left: Int, right: Int, top: Int, bottom: Int, mask: Option[Array[Array[Int]]] = None)
  case class KeyboardLayout(brightnessThreshold: Double, smoothAverage: Double, cutY: Int, layout: List[KeyBoundingBox])

  def extractLayoutFromImage(
    src: BufferedImage,
    brightnessThreshold: Double,
    smoothAverage: Double,
    cutY: Int
  ): KeyboardLayout = {
    val image = Image.fromAwt(src)
    val data =
      extractNormalizedBlackNotesPattern(
        img = image,
        y = cutY,
        threshold = (255 * brightnessThreshold).toInt,
        averageFactor = (image.width * smoothAverage).toInt
      )

    val l = localMaximums(data, image.width / 35, brightnessThreshold)
    val m = minimumDistance(l)
    val a = associateNoteAndPatternI(l, m)
    val c = findCPositions(a)
    val b = keyboardHBounds(c)
    val avgB = averageHorizontalBright(image)
    val top = topBrightWithChange(avgB, cutY, brightnessThreshold).getOrElse(0)
    val bot = bottomBrightWithChange(avgB, cutY, brightnessThreshold).getOrElse(image.height)
    val kb = hBoundToBound(b, top, bot)
    val kbWithMask = applyMask(image, (255 * brightnessThreshold).toInt, kb)

    KeyboardLayout(brightnessThreshold, smoothAverage, cutY, kbWithMask)
  }

  def paintLayoutActiveNotes(img: BufferedImage, keyboardLayout: KeyboardLayout, activeNotes: List[KeyboardNote]): BufferedImage = {
    if(keyboardLayout != null) {
      val c = new Color(100, 100, 255, 128)

      keyboardLayout
        .layout
        .filter(kl => activeNotes.contains(kl.key))
        .foreach { k =>
          paintNoteDifferential(img, img, k, c)
        }
    }
    img
  }

  def paintFullLayout(
    src: BufferedImage,
    dst: BufferedImage,
    keyboarLayout: KeyboardLayout
  ): Unit = {
    keyboarLayout.layout.foreach { bound =>
      paintNoteDifferential(
        src = src,
        dst = dst,
        k = bound,
        c = bound.key.note match {
          case MusicNote.C => new Color(255, 0, 0, 128)
//          case MusicNote.`C#-Db` => new Color(50, 0, 0, 128)
          case MusicNote.D => new Color(0, 255, 0, 128)
//          case MusicNote.`D#-Eb` => new Color(0, 50, 0, 128)
          case MusicNote.E => new Color(0, 0, 255, 128)
          case MusicNote.F => new Color(255, 255, 0, 128)
//          case MusicNote.`F#-Gb` => new Color(50, 50, 0, 128)
          case MusicNote.G => new Color(0, 255, 255, 128)
//          case MusicNote.`G#-Ab` => new Color(0, 50, 50, 128)
          case MusicNote.A => new Color(255, 0, 255, 128)
//          case MusicNote.`A#-Bb` => new Color(50, 0, 50, 128)
          case MusicNote.B => new Color(255, 255, 255, 128)
          case _ => new Color(0, 0, 0, 0)
        }
      )
    }
  }

  private def paintNoteDifferential(src: BufferedImage, dst: BufferedImage, k: KeyBoundingBox, c: Color) = {
    k.mask.foreach { mask =>
      (k.left until k.right).foreach { x =>
        (k.top until k.bottom).foreach { y =>
          if (x > 0 && x < dst.getWidth && y > 0 && y < dst.getHeight) {
            val m = mask(x - k.left)(y - k.top)
            if(m != -1) {
              val rgb = src.getRGB(x, y)
              val grayScale = ((rgb >> 16 & 0xFF) + (rgb >> 8 & 0xFF) + (rgb & 0xFF)) / 3
              if (Math.abs(grayScale - m) < 50) {
                dst.setRGB(x, y, c.getRGB)
              }
            }
          }
        }
      }
    }
  }

  private def applyMask(img: Image, brightnessThreshold: Int, kb: List[KeyBoundingBox]) = {
    val binImage = img.filter(ThresholdFilter(brightnessThreshold))
    kb.map { k =>
      val mask = ArrayBuffer.fill[Int](k.right - k.left + 1, k.bottom - k.top + 1)(-1)
      (k.left to k.right).foreach {x =>
        (k.top to k.bottom).foreach { y =>
          if(x > 0 && x < binImage.width && y > 0 && y < binImage.height) {
            val binPixel = binImage.pixel(x, y)
            val grayScale = (binPixel.red + binPixel.blue + binPixel.green)/3
            val srcPixel = img.pixel(x, y)
            val srcGayScale = (srcPixel.red + srcPixel.blue + srcPixel.green)/3
            if (k.key.note.isUpperNote && grayScale < 200) {
              mask(x - k.left)(y - k.top) = srcGayScale
            } else if (!k.key.note.isUpperNote && grayScale > 200) {
              mask(x - k.left)(y - k.top) = srcGayScale
            }
          }
        }
      }
      k.copy(mask = Some(mask.map(_.toArray).toArray))
    }
  }

  private def hBoundToBound(bounds: List[KeyHBoundingBox], top: Int, bottom: Int): List[KeyBoundingBox] = {
    bounds.map { hBound =>
      KeyBoundingBox(hBound.key, hBound.left, hBound.right, top, bottom)
    }
  }

  private def topBrightWithChange(averageHorizontalBright: Seq[Double], cutY: Int, darker: Double): Option[Int] = {
    val cutBright = averageHorizontalBright(cutY)*(1 - darker)
    averageHorizontalBright
      .zipWithIndex
      .collect {
        case (bright, index) if index < cutY => (bright, index)
      }
      .reverse
      .find(_._1 < cutBright)
      .map(_._2)
  }

  private def bottomBrightWithChange(averageHorizontalBright: Seq[Double], cutY: Int, darker: Double): Option[Int] = {
    val cutBright = averageHorizontalBright(cutY)*(1 - darker)
    averageHorizontalBright
      .zipWithIndex
      .collect {
        case (bright, index) if index > cutY => (bright, index)
      }
      .find(_._1 < cutBright)
      .map(_._2)
  }

  private def averageHorizontalBright(image: Image): Seq[Double] = {
    (0 until image.height)
      .map { i =>
        (0 until image.width).map(j => image.pixel(j, i).red).sum.toDouble / image.width
      }
  }

  private def keyboardHBounds(cPositions: Map[KeyboardNote, Int]): List[KeyHBoundingBox] = {
    if(cPositions.size <= 1) {
      throw new Exception("No C positions (or a single one) found for the specified inputs.")
    }
    if(!cPositions.forall(_._1.note == MusicNote.C)) {
      throw new Exception("Some of the found Key positions don't correspond to a C key. ")
    }

    val sortedCPositions =
      cPositions
        .toList
        .sortBy(_._2)

    val leftOctaveWidth = sortedCPositions(1)._2 - sortedCPositions.head._2
    val rightOctaveWidth = sortedCPositions.last._2 - sortedCPositions(sortedCPositions.size - 2)._2
    val appendLeft =
      KeyboardNote(MusicNote.C, sortedCPositions.head._1.index - 1) -> (sortedCPositions.head._2 - leftOctaveWidth)
    val appendRight =
      KeyboardNote(MusicNote.C, sortedCPositions.last._1.index + 1) -> (sortedCPositions.last._2 + rightOctaveWidth)

    val extendedCPositions = appendLeft +: sortedCPositions :+ appendRight

    extendedCPositions
      .dropRight(1)
      .zipWithIndex
      .flatMap {
        case ((keyboardNote, position), index) =>
          octaveHBounds(keyboardNote.index, position, extendedCPositions(index + 1)._2)
      }
  }

  /**
    * Width(BK) = 0.6 * Width(WK)
    * Gh = 0.6 * Width(WK)
    * Offset(C#-Db) = X(C) + Gh
    * Offset(D#-Eb) = X(F) - Gh - Width(BK)
    * Offset(F#-Gb) = X(F) + Gh
    * Offset(G#-Ab) = X(A) - Width(BK)/2
    * Offset(A#-Bb) = X(C') - Gh
    */
  private def octaveHBounds(keyIndex: Int, left: Int, right: Int): List[KeyHBoundingBox] = {
    if(left >= right) {
      throw new Exception("Left and right bounds were incorrect while calculating the Octave bounds.")
    }

    val w = (right - left).toDouble
    val whiteNoteW = w / 7
    val blackNoteW = 0.6 * whiteNoteW
    val gh = 0.6 * whiteNoteW

    List(
      (MusicNote.C, left + whiteNoteW * 0, left + whiteNoteW * 1),
      (MusicNote.D, left + whiteNoteW * 1, left + whiteNoteW * 2),
      (MusicNote.E, left + whiteNoteW * 2, left + whiteNoteW * 3),
      (MusicNote.F, left + whiteNoteW * 3, left + whiteNoteW * 4),
      (MusicNote.G, left + whiteNoteW * 4, left + whiteNoteW * 5),
      (MusicNote.A, left + whiteNoteW * 5, left + whiteNoteW * 6),
      (MusicNote.B, left + whiteNoteW * 6, left + whiteNoteW * 7),
      (MusicNote.`C#-Db`, left + gh, left + gh + blackNoteW),
      (MusicNote.`D#-Eb`, (left + whiteNoteW * 3) - gh - blackNoteW, (left + whiteNoteW * 3) - gh),
      (MusicNote.`F#-Gb`, (left + whiteNoteW * 3) + gh, (left + whiteNoteW * 3) + gh + blackNoteW),
      (MusicNote.`G#-Ab`, (left + whiteNoteW * 5) - blackNoteW / 2, (left + whiteNoteW * 5) + blackNoteW / 2),
      (MusicNote.`A#-Bb`, (left + whiteNoteW * 7) - gh - blackNoteW, (left + whiteNoteW * 7) - gh)
    ).map { case (note, keyLeft, keyRight) =>
      KeyHBoundingBox(
        key = KeyboardNote(note, keyIndex),
        left = keyLeft.toInt,
        right = keyRight.toInt
      )
    }
  }

  private def findCPositions(data: Map[PatternI, Option[MusicNote]]): Map[KeyboardNote, Int] = {
    val sortedData =
      data
        .toList
        .sortBy(_._1.x)

    val cPositions =
      sortedData
        .zipWithIndex
        .collect {
          case ((pattern, note), index)
            if (index + 1) < sortedData.size &&
              note.contains(MusicNote.`A#-Bb`) &&
              sortedData(index + 1)._2.contains(MusicNote.`C#-Db`) =>
            (MusicNote.C, (pattern.x + sortedData(index + 1)._1.x)/2)
        }

    val middleCIndex = cPositions.size/2

    cPositions
      .zipWithIndex
      .map {case ((musicNote, position), index) =>
        KeyboardNote(musicNote, index - middleCIndex + 4) -> position
      }
      .toMap
  }

  private def minimumDistance(data: Seq[PatternI]): Double = {
    if(data.isEmpty || data.size == 1) {
      Double.MaxValue
    } else {
      Math.min(
        data.tail.map(x => Math.abs(x.x - data.head.x)).min,
        minimumDistance(data.tail)
      )
    }
  }

  private def associateNoteAndPatternI(data: Seq[PatternI], minDistance: Double): Map[PatternI, Option[MusicNote]] = {
    if(data.size < 2) {
      throw new Exception("Pattern data is not enough to calculate Keyboard Layout association.")
    }

    val sortedData = data.sortBy(_.x)
    List(MusicNote.`C#-Db`, MusicNote.`D#-Eb`, MusicNote.`F#-Gb`, MusicNote.`G#-Ab`, MusicNote.`A#-Bb`)
      .map { startNote =>
        Map(sortedData.head -> Some(startNote)) ++ assumingLast(sortedData.head.x, Some(startNote), sortedData.tail, minDistance)
      }
      .map { m =>
        (m, m.count(_._2.isDefined))
      }
      .maxBy(_._2)
      ._1
  }

  private def assumingLast(lastAssumedX: Int, lastAssumedNote: Option[MusicNote], data: Seq[PatternI], minDistance: Double): Map[PatternI, Option[MusicNote]] = {
    if(data.isEmpty) {
      Map.empty
    } else {
      val currentNote =
        if(isDb(data.head.x, lastAssumedX, lastAssumedNote, minDistance)) Some(MusicNote.`C#-Db`)
        else if(isEb(data.head.x, lastAssumedX, lastAssumedNote, minDistance)) Some(MusicNote.`D#-Eb`)
        else if(isGb(data.head.x, lastAssumedX, lastAssumedNote, minDistance)) Some(MusicNote.`F#-Gb`)
        else if(isAb(data.head.x, lastAssumedX, lastAssumedNote, minDistance)) Some(MusicNote.`G#-Ab`)
        else if(isBb(data.head.x, lastAssumedX, lastAssumedNote, minDistance)) Some(MusicNote.`A#-Bb`)
        else None
      Map(data.head -> currentNote) ++ assumingLast(data.head.x, currentNote, data.tail, minDistance)
    }
  }

  private def isDb(x: Int, prevX: Int, prevNote: Option[MusicNote], minDistance: Double) = prevNote.contains(MusicNote.`A#-Bb`) && (x - prevX) > (minDistance * 2)*0.75 && (x - prevX) < (minDistance * 2)*1.25
  private def isEb(x: Int, prevX: Int, prevNote: Option[MusicNote], minDistance: Double) = prevNote.contains(MusicNote.`C#-Db`) && (x - prevX) > minDistance*0.75 && (x - prevX) < minDistance*1.25
  private def isGb(x: Int, prevX: Int, prevNote: Option[MusicNote], minDistance: Double) = prevNote.contains(MusicNote.`D#-Eb`) && (x - prevX) > (minDistance * 2)*0.75 && (x - prevX) < (minDistance * 2)*1.25
  private def isAb(x: Int, prevX: Int, prevNote: Option[MusicNote], minDistance: Double) = prevNote.contains(MusicNote.`F#-Gb`) && (x - prevX) > minDistance*0.75 && (x - prevX) < minDistance*1.25
  private def isBb(x: Int, prevX: Int, prevNote: Option[MusicNote], minDistance: Double) = prevNote.contains(MusicNote.`G#-Ab`) && (x - prevX) > minDistance*0.75 && (x - prevX) < minDistance*1.25

  private def localMaximums(data: Seq[PatternI], grid: Int, threshold: Double): Seq[PatternI] = {
    val aboveThresholdValues = data.filter(_.i > threshold)
    aboveThresholdValues match {
      case x if x.isEmpty => Seq.empty
      case _ =>
        val max = aboveThresholdValues.maxBy(_.i)
        Seq(max) ++ localMaximums(data.filter(p => (p.x < max.x - grid) || (p.x > max.x + grid)), grid, threshold)
    }
  }

  private def extractNormalizedBlackNotesPattern(img: Image, y: Int, threshold: Int, averageFactor: Int): Seq[PatternI] = {
    val rawPattern =
      img
        .filter(ThresholdFilter(threshold))
        .subimage(0, y, img.width, 1)
        .pixels
        .map(x => (255.0 - x.red)/255.0)

    averageSmooth(
      list = rawPattern,
      sFactor = averageFactor
    )
      .zipWithIndex
      .map {
        case (d, index) => PatternI(index, d)
      }
  }

  private def averageSmooth(list: Seq[Double], sFactor: Int): Seq[Double] = {
    list.zipWithIndex.map { case (lx, index) =>
      val sValues =
        ((index - sFactor) to (index + sFactor))
          .filter(i => i >= 0 && i < list.size)
          .map(list(_))
      sValues.sum/(sValues.size + 1)
    }
  }
}

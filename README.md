# ZigZag
ZigZag is both an image binarization and background removal algorithm.
ZigZag is the follow-up to YinYang, our previous image binarization algorithm.
ZigZag is faster, more precise and simpler than YinYang.

The [zigzag.jar](zigzag.jar) java archive is generated periodically from the source code for convenience.
Be careful, as it may not be the latest version.


### Requires:
- Java 8 (that's all)

### How to Run ZigZag
- **By calling Java API**

  `new ZigZagFilter(localWindowSize, meanPercentage, filterMode).filter(inputImagePath, inputImagePath);`

  Example:  

  `new ZigZagFilter(25, 100, ImageBinarizer.MODE_GRAY_LEVEL).filter("C:/MyImage.jpg", "C:/MyBinarizedImage.png");`


- **By launching Java runtime**

  `java -classpath zigzag.jar sugarcube.imapro.zigzag.ZigZagFilter localWindowSize meanPercentage filterMode inputImagePath outputImagePath`

   Example:

  `java -classpath zigzag.jar sugarcube.imapro.zigzag.ZigZagFilter 25 100 3 "C:/MyImage.jpg" "C:/MyBinarizedImage.png"`

### ZigZag Parameters
- *localWindowSize* - typical values can range from 10 to 100 pixels, by default you can try 25
- *meanPercentage* - typical values can range from 50 to 100 percent, a default value of 100 is recommended
- *filterMode* - an integer value in the set: 0,1,2,3,4

### Filter Modes
- 0 - Standard binarization mode
- 1 - Upsampled binarization mode (x2)
- 2 - Antialiased binarization mode
- 3 - Background removal, graylevel foreground
- 4 - Background removal, color foreground

### Not Yet Official ZigZag Evaluation
The evaluation was done using the Wezut photographed document image dataset:
- we first applied the binarization algorithms to the dataset images
- then we applied Google OCR (com.google.cloud.vision.v1)
- finally we compared the result to the ground-truth

| Algorithm | Millis | Precision | Recall | F1-Score | Levenshtein |
|-----------|--------|-----------|--------|----------|-------------|
| ZigZag GL | 297    | 99.98     | 99.98  | 99.98    | 99.95       |
| ZigZag BW | 159    | 99.91     | 99.89  | 99.9     | 99.77       |
| YinYang   | 898    | 99.89     | 99.8   | 99.84    | 99.7        |
| ZigYang   | 150    | 99.85     | 99.81  | 99.83    | 99.65       |
| Nick      | 1262   | 99.81     | 99.76  | 99.79    | 99.53       |
| Sauvola   | 1429   | 99.82     | 99.77  | 99.79    | 99.64       |
| Bradley   | 159    | 99.68     | 99.74  | 99.71    | 99.59       |
| Michalak  | 81     | 99.76     | 99.62  | 99.69    | 99.44       |
| Bersnen   | 599    | 91.38     | 99.57  | 95.21    | 88.28       |
| Niblack   | 2431   | 76.65     | 99.32  | 86.23    | 65.35       |
| Otsu      | 26     | 98.93     | 60.8   | 73.43    | 59.66       |

### Citation
If you use this code for your research, please cite the following paper ([available on ACM Open Access](https://dl.acm.org/doi/10.1145/3573128.3609354)):
```
@inproceedings{10.1145/3573128.3609354,
author = {Bloechle, Jean-Luc and Hennebert, Jean and Gisler, Christophe},
title = {YinYang, a Fast and Robust Adaptive Document Image Binarization for Optical Character Recognition},
year = {2023},
isbn = {9798400700279},
publisher = {Association for Computing Machinery},
address = {New York, NY, USA},
url = {https://doi.org/10.1145/3573128.3609354},
doi = {10.1145/3573128.3609354},
abstract = {Optical Character Recognition (OCR) from document photos taken by cell phones is a challenging task. Most OCR methods require prior binarization of the image, which can be difficult to achieve when documents are captured with various mobile devices in unknown lighting conditions. For example, shadows cast by the camera or the camera holder on a hard copy can jeopardize the binarization process and hinder the next OCR step. In the case of highly uneven illumination, binarization methods using global thresholding simply fail, and state-of-the-art adaptive algorithms often deliver unsatisfactory results. In this paper, we present a new binarization algorithm using two complementary local adaptive passes and taking advantage of the color components to improve results over current image binarization methods. The proposed approach gave remarkable results at the DocEng'22 competition on the binarization of photographed documents.},
booktitle = {Proceedings of the ACM Symposium on Document Engineering 2023},
articleno = {19},
numpages = {4},
keywords = {image thresholding, image processing, binarization, OCR},
location = {Limerick, Ireland},
series = {DocEng '23}
}
```
ZigZag paper is underway, in the meantime, citing YinYang paper is just fine :-)

### ZigZag is provided under the terms of GPLv3
The GNU General Public License version 3 or GPLv3 is a copyleft license, which means that any derivative works (modifications or extensions) of a GPL-licensed software must also be licensed under the GPL-3 or a compatible license. This requirement ensures that open-source code remains open source.
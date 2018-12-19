package worldgenerator.geometry;

import java.util.Random;

public class MathUtils {

    public static boolean closeEnough(double d1, double d2, double diff) {
        return Math.abs(d1 - d2) <= diff;
    }
    
    public static double distance(Point p1, Point p2) {
        return Math.sqrt(((p1.x - p2.x) * (p1.x - p2.x)) + ((p1.y - p2.y) * (p1.y - p2.y)));
    }
    
    public static Vector2D randomUnitVector(Random r){
    	double theta = r.nextDouble() * (Math.PI * 2);
    	double phi = Math.acos(r.nextBoolean() ? r.nextDouble()*-1: r.nextDouble()*1);
    	double sinPhi = Math.sin(phi);
    	return new Vector2D(
    		Math.cos(theta) * sinPhi,
    		Math.sin(theta) * sinPhi);
    }
    
    public static class Vector2D extends Point {
    	
    	public Vector2D(double x, double y) {
    		super(x, y);
    	}
    	
    	public Vector2D(Point p) {
    		super(p.x, p.y);
    	}
    	
    	public double gradient() {
    		return (y)/(x);
    	}
    	
    	public double gradientFrom(Point p) {
    		return (y-p.y)/(x-p.x);
    	}
    	
    	public Vector2D setLength(double length) {
			return new Vector2D(x + (Math.cos(gradient()) * length), y + (Math.sin(gradient()) * length));
		}

		public Vector2D crossProduct() {
			double 
				x1 = x,
				x2 = 0,
				y1 = y,
				y2 = 0;
			
			//find the center
			double cx = (x1+x2)/2;
			double cy = (y1+y2)/2;

			//move the line to center on the origin
			x1-=cx; y1-=cy;
			x2-=cx; y2-=cy;

			//rotate both points
			double 
				xtemp = x1,
				ytemp = y1;
			x1=-ytemp; 
			y1=xtemp; 

			xtemp = x2; 
			ytemp = y2;
			
			x2=-ytemp; 
			y2=xtemp; 

			//move the center point back to where it was
			x1+=cx; y1+=cy;
			x2+=cx; y2+=cy;
			return new Vector2D(x1, y1);
		}
		
		public Vector2D projectOnVector(Vector2D vector) {
			return projectVectors(this, vector);
		}
		
		public static Vector2D projectVectors(Vector2D vector1, Vector2D vector2) {
			return vector1.setLength(vector1.dot(vector2.unitVector()));
		}
		
		public Vector2D unitVector() {
			return new Vector2D(x/length(), y/length());
		}
		
		public double dot(Vector2D vector) {
			return (x*y) + (vector.x*vector.y);
		}

		public void add(Vector2D vector) {
			x += vector.x;
			y += vector.y;
		}
		
		public void sub(Vector2D vector) {
			x -= vector.x;
			y -= vector.y;
		}
		
		public Vector2D addTo(Vector2D vector) {
			return new Vector2D(x+vector.x, y+vector.y);
		}
		
		public Vector2D subFrom(Vector2D vector) {
			return new Vector2D(x-vector.x, y-vector.y);
		}
    }
    
    public static class NoiseGenerator {
    	private int octaveCount, noiseWidth, noiseHeight;
        private final Random r;

        /**
         * @param random Randomizer.
         * @param octaveCount Smooth value. 0 means there will be only white noise.
         * @param noiseWidth Width of noise map. Should be less than graph width.
         * @param noiseHeight Height of noise map. Should be less than graph width.
         */
        public NoiseGenerator(Random random, int octaveCount, int noiseWidth, int noiseHeight) {
            this.r = random;
            this.octaveCount = octaveCount;
            this.noiseWidth = noiseWidth;
            this.noiseHeight = noiseHeight;
        }
        
        public double[][] getWhiteNoise(){
        	return generateWhiteNoise(noiseWidth + 1, noiseHeight + 1);
        }
        
        public double[][] getPerlinNoise(){
        	return generatePerlinNoise(getWhiteNoise(), octaveCount);
        }
        
        public double[][] getSmoothWhiteNoise(){
        	return generateSmoothNoise(getWhiteNoise(), octaveCount);
        }
        
        public double[][] getSmoothPerlinNoise(){
        	return generateSmoothNoise(getPerlinNoise(), octaveCount);
        }
        
        public double getMedian(float[][] noise) {
        	return findMedian(noise);
        }

        /**
         * @return The value dividing ground and water.
         */
        private double findMedian(float[][] noise) {
            long count[] = new long[10];

            for (float[] aNoise : noise) {
                for (float aaNoise : aNoise) {
                    for (int k = 1; k < count.length; k++) {
                        if (aaNoise * 10 < k) {
                            count[k - 1]++;
                            break;
                        }
                    }
                }
            }

            int n = 0;

            for (int i = 1; i < count.length; i++) {
                if (count[i] > count[n]) n = i;
            }

            return (float) (n + 0.5) / 10;
        }

        /**
         *
         * @param width Width of noise map.
         * @param height height of noise map.
         * @return White noise map.
         */
        private double[][] generateWhiteNoise(int width, int height) {
        	double[][] noise = new double[width][height];

            for (int i = width / 25; i < width * 0.96; i++) {
                for (int j = height / 25; j < height * 0.96; j++) {
                    noise[i][j] = r.nextFloat();
                }
            }

            return noise;
        }

        /**
         *
         * @param whiteNoise2 Noise map which will be processed.
         * @param octaveCount Smooth value. 0 means there will be only white noise.
         * @return Perlin noise.
         */
        private double[][] generatePerlinNoise(double[][] whiteNoise2, int octaveCount) {
            int width = whiteNoise2.length;
            int height = whiteNoise2[0].length;

            double[][][] smoothNoise = new double[octaveCount][][]; //an array of 2D arrays containing

            float persistance = 0.5f;

            //generate smooth noise
            for (int i = 0; i < octaveCount; i++) {
                smoothNoise[i] = generateSmoothNoise(whiteNoise2, i);
            }

            double[][] perlinNoise = new double[width][height];
            float amplitude = 1.0f;
            float totalAmplitude = 0.0f;

            //blend noise together
            for (int octave = octaveCount - 1; octave >= 0; octave--) {
                amplitude *= persistance;
                totalAmplitude += amplitude;

                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        perlinNoise[i][j] += smoothNoise[octave][i][j] * amplitude;
                    }
                }
            }

            //normalisation
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    perlinNoise[i][j] /= totalAmplitude;
                }
            }

            return perlinNoise;
        }

        /**
         *
         * @param whiteNoise2 Noise map which will be used to create a smoother noise map.
         * @param octave Smooth value. 0 means there will be only white noise.
         * @return Smoother noise map than a base map.
         */
        private double[][] generateSmoothNoise(double[][] whiteNoise2, int octave) {
            int width = whiteNoise2.length;
            int height = whiteNoise2[0].length;

            double[][] smoothNoise = new double[width][height];

            int samplePeriod = 1 << octave; // calculates 2 ^ k
            float sampleFrequency = 1.0f / samplePeriod;

            for (int i = 0; i < width; i++) {
                //calculate the horizontal sampling indices
                int sample_i0 = (i / samplePeriod) * samplePeriod;
                int sample_i1 = (sample_i0 + samplePeriod) % width; //wrap around
                float horizontal_blend = (i - sample_i0) * sampleFrequency;

                for (int j = 0; j < height; j++) {
                    //calculate the vertical sampling indices
                    int sample_j0 = (j / samplePeriod) * samplePeriod;
                    int sample_j1 = (sample_j0 + samplePeriod) % height; //wrap around
                    float vertical_blend = (j - sample_j0) * sampleFrequency;

                    //blend the top two corners
                    double top = interpolate(whiteNoise2[sample_i0][sample_j0],
                            whiteNoise2[sample_i1][sample_j0], horizontal_blend);

                    //blend the bottom two corners
                    double bottom = interpolate(whiteNoise2[sample_i0][sample_j1],
                            whiteNoise2[sample_i1][sample_j1], horizontal_blend);

                    //final blend
                    smoothNoise[i][j] = interpolate(top, bottom, vertical_blend);
                }
            }

            return smoothNoise;
        }

        /**
         * Function returns a linear interpolation between two values.
         * Essentially, the closer alpha is to 0, the closer the resulting value will be to x0;
         * the closer alpha is to 1, the closer the resulting value will be to x1.
         *
         * @param whiteNoise2 First value.
         * @param whiteNoise22 Second value.
         * @param alpha Transparency.
         * @return Linear interpolation between two values.
         */
        private static double interpolate(double whiteNoise2, double whiteNoise22, float alpha) {
            return whiteNoise2 * (1 - alpha) + alpha * whiteNoise22;
        }
    }
}

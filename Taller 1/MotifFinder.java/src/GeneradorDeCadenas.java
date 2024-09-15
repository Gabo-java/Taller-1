import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class GeneradorDeCadenas {

    static class GeneradorDeCadena extends RecursiveTask<String[]> {
        private final int start, end, length;
        private final double[] probabilities;

        public GeneradorDeCadena(int start, int end, int length, double[] probabilities) {
            this.start=start;
            this.end=end;
            this.length=length;
            this.probabilities=probabilities;
        }

        @Override
        protected String[] compute() {
            if (end - start <= 100000) { 
                return GenerarSecuenciasACGT(start, end, length, probabilities);
            } else {
                int mid=(start+end)/2;
                GeneradorDeCadena leftTask=new GeneradorDeCadena(start, mid, length, probabilities);
                GeneradorDeCadena rightTask=new GeneradorDeCadena(mid, end, length, probabilities);
                invokeAll(leftTask, rightTask);
                String[] leftResult=leftTask.join();
                String[] rightResult=rightTask.join();
                String[] result=new String[leftResult.length+rightResult.length];
                System.arraycopy(leftResult, 0, result, 0, leftResult.length);
                System.arraycopy(rightResult, 0, result, leftResult.length, rightResult.length);
                return result;
            }
        }
    }

    public static String[] GenerarSecuenciasACGT(int start, int end, int length, double[] probabilities) {
        String[] sequences=new String[end - start];
        char[] bases={'A', 'C', 'G', 'T' };
        Random random=new Random();
    
        for (int i=0; i<sequences.length; i++) {
            StringBuilder sequence=new StringBuilder(length);
            for (int j=0; j<length; j++) {
                double rand=random.nextDouble();
                if (rand<=probabilities[0]) {
                    sequence.append(bases[0]);  
                } else if (rand <= probabilities[0]+probabilities[1]) {
                    sequence.append(bases[1]);  
                } else if (rand <= probabilities[0]+probabilities[1]+probabilities[2]) {
                    sequence.append(bases[2]);
                } else {
                    sequence.append(bases[3]);
                }
            }
            sequences[i]=sequence.toString();
        }
        return sequences;
    }    

    public static void GuardarSecuenciasEnArchivo(String[] secuencias, String nombreArchivo) throws IOException {
        FileWriter writer=new FileWriter(nombreArchivo);
        int contador=1;
        for (String secuencia:secuencias) {
            writer.write("("+contador+")"+secuencia+"\n");
            contador++;
        }
        writer.close();
    }

    public static String EncontrarMotif(String[] secuencias, int tamanoMotif) {
        HashMap<String, Integer> conteoMotifs=new HashMap<>();
        for (String secuencia:secuencias) {
            for (int i=0; i<=secuencia.length()-tamanoMotif; i++) {
                String motif=secuencia.substring(i, i+tamanoMotif);
                conteoMotifs.put(motif, conteoMotifs.getOrDefault(motif, 0) + 1);
            }
        }
        String motifMasFrecuente="";
        int maxConteo=0;
        for (Map.Entry<String, Integer> entry:conteoMotifs.entrySet()) {
            if (entry.getValue()>maxConteo) {
                motifMasFrecuente=entry.getKey();
                maxConteo=entry.getValue();
            }
        }
        return motifMasFrecuente;
    }

    public static double EntropiaDeShannon(String secuencia) {
        int[] frecuencias=new int[4];
        for (char base:secuencia.toCharArray()) {
            switch (base) {
                case 'A':
                    frecuencias[0]++;
                    break;
                case 'C':
                    frecuencias[1]++;
                    break;
                case 'G':
                    frecuencias[2]++;
                    break;
                case 'T':
                    frecuencias[3]++;
                    break;
            }
        }
        double entropia=0.0;
        for (int count:frecuencias) {
            if (count>0) {
                double probabilidad=(double) count/secuencia.length();
                entropia-=probabilidad*(Math.log(probabilidad)/Math.log(2));
            }
        }
        return entropia;
    }

    public static String[] FiltrarPorEntropia(String[] secuencias, double umbral) {
        return java.util.Arrays.stream(secuencias)
                .filter(secuencia->EntropiaDeShannon(secuencia) >= umbral)
                .toArray(String[]::new);
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner=new Scanner(System.in);

        int n;
        do {
            System.out.print("Enter the number of sequences:");
            n=scanner.nextInt();
        } while (n<1000 || n>2000000);

        int m;
        do {
            System.out.print("Enter the length of each sequence:");
            m=scanner.nextInt();
        } while (m<5 || m>100);

        int s;
        do {
            System.out.print("Enter the motif size:");
            s = scanner.nextInt();
        } while (s<4 || s>10);

        double[] probabilidades={ 0.25, 0.25, 0.25, 0.25 }; 
        double umbralEntropia=1.95;

        ForkJoinPool pool=new ForkJoinPool(Runtime.getRuntime().availableProcessors());

        GeneradorDeCadena task=new GeneradorDeCadena(0, n, m, probabilidades);
        String[] secuencias=pool.invoke(task);

        GuardarSecuenciasEnArchivo(secuencias, "Database");

        long tiempoInicio=System.currentTimeMillis();

        String motif=EncontrarMotif(secuencias, s);
        System.out.println("Most frequent motif:"+motif);

        long tiempoFin=System.currentTimeMillis(); 
        long tiempoTotal=tiempoFin-tiempoInicio; 

        System.out.println("Total time:"+tiempoTotal+"ms");
        
        long tiempoInicio2=System.currentTimeMillis();

        String[] secuenciasFiltradas=FiltrarPorEntropia(secuencias, umbralEntropia);
        System.out.println("Number of sequences after entropy filtering:"+secuenciasFiltradas.length);

        String motifFiltrado=EncontrarMotif(secuenciasFiltradas, s);
        System.out.println("Most frequent motif in filtered sequences:"+motifFiltrado);

        long tiempoFin2=System.currentTimeMillis(); 
        long tiempoTotal2=tiempoFin2-tiempoInicio2; 
        System.out.println("Total time:"+tiempoTotal2+"ms");

        scanner.close();
    }
}

package com.pcagrade.order.algo;


/* TD1 ex. 3. Calcul d'un multi sous-ensemble de l'ensemble X, de somme S.
Abréviation : mse = multi sous-ensemble
e(k,s) est la vérité de la proposition "il existe un mse du k-préfixe de X dont la somme est s."
En particulier : e(n,S) est la vérité de "il existe un mse de X dont la somme est S."

Equation de récurrence des valeurs e(k,s)
-----------------------------------------
BASE k = 0 : ...
HEREDITE 1 ≤ k < n+1 : ...
*/
import java.util.Arrays;
public class Exercice3{ // TD1, ex. 3 : multi sous-ensemble de somme S
    public static void main(String[] args){ int nargs = args.length;
        // Entrée du programme : les éléments de X suivis de la somme S
        if (nargs == 0){ System.out.println("Usage : x_{0} ... x_{n-1} S");
            return;
        }
        int S = Integer.parseInt(args[nargs-1]);
        int n = nargs - 1; // nombre d'éléments de X
        int[] X = new int[n];
        for (int i = 0; i < n; i++) X[i] = Integer.parseInt(args[i]);
        System.out.printf("\nX = %s, S = %d\n", Arrays.toString(X), S);

        // Calcul de E, de terme général e(k,s)
        boolean[][] E = calculerE(X, S);
        System.out.println("E :");
        afficher(E);
        if (!E[n][S]){
            System.out.printf("Il n'y a pas de multi-sous-ensemble de %s de somme %s\n\n",
                    Arrays.toString(X), S);
            return;
        }
        System.out.printf("Un multi-sous-ensemble de %s de somme %d : ",
                Arrays.toString(X), S
        );
        System.out.print("{ ");
        amse(E, X, n, S); // affiche un multi sous-ensemble de X, de somme S.
        System.out.print("}");
        System.out.println();
    }
    static boolean[][] calculerE(int[] X, int S){ int n = X.length;
        boolean[][] E = new boolean[n+1][S+1];
        // base k=0
        E[0][0]=true;
        for(int s=1; s<S+1;s++)
            E[0][s]=false;
        // hérédité
        for(int k=1;k<n+1;k++)
            for(int s=0; s<S+1;s++)
                if(s-X[k-1]<0)
                    E[k][s]=E[k-1][s];
                else
                    E[k][s]=E[k-1][s] || E[k][s-X[k-1]];

        return E;
    }
    static void afficher(boolean[][] T){int m = T.length;
        for (int i = m-1; i > -1; i--)
            System.out.println(Arrays.toString(T[i]));
    }
    static void amse(boolean[][] E, int[] X, int k, int s){
        /* Affiche un multi-sous-ensemble de X[0:k] de somme s. */
        if(k==0 && s==0)return;
        if(E[k-1][s])
            amse(E,X,k-1,s);
        else {
            amse(E,X, k, s-X[k-1]);
            System.out.printf("X[%d]=%d\n",k-1,X[k-1]);
        }

    }
}
/* Compilation et exécution dans un terminal Unix.
% javac Exercice3.java
% java Exercice3 5 2 3 12

X = [5, 2, 3], S = 12
E :
[true, false, true, true, true, true, true, true, true, true, true, true, true]
[true, false, true, false, true, true, true, true, true, true, true, true, true]
[true, false, false, false, false, true, false, false, false, false, true, false, false]
[true, false, false, false, false, false, false, false, false, false, false, false, false]
Un multi-sous-ensemble de [5, 2, 3] de somme 12 : { 5,5,2,}
% java Exercice3 3 2 5 12

X = [3, 2, 5], S = 12
E :
[true, false, true, true, true, true, true, true, true, true, true, true, true]
[true, false, true, true, true, true, true, true, true, true, true, true, true]
[true, false, false, true, false, false, true, false, false, true, false, false, true]
[true, false, false, false, false, false, false, false, false, false, false, false, false]
Un multi-sous-ensemble de [3, 2, 5] de somme 12 : { 3,3,3,3,}
% java Exercice3 3 2 5  1

X = [3, 2, 5], S = 1
E :
[true, false]
[true, false]
[true, false]
[true, false]
Il n'y a pas de multi-sous-ensemble de [3, 2, 5] de somme 1

% java Exercice3   0

X = [], S = 0
E :
[true]
Un multi-sous-ensemble de [] de somme 0 : { }
%
*/
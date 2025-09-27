import { ApiService } from '../services/api';

declare global {
  interface Window {
    apiService?: ApiService;
  }
  // Définir l'interface Order
  interface Order {
    id: string;
    orderNumber: string;
    reference: string;
    cardCount: number;
    totalPrice: number;
    priority: 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW';
    status: number;
    statusText: string;
    estimatedTimeMinutes: number;
    estimatedTimeHours: number;
    creationDate: string;
    orderDate: string;
    deadline: string;
    qualityIndicator: number;
    minimumGrade: number;
    type: string;
    unsealing: boolean;
  }
// Constantes côté frontend
  export const ORDER_STATUS = {
    A_RECEPTIONNER: 1,
    COLIS_ACCEPTE: 9,
    A_SCANNER: 10,
    A_OUVRIR: 11,
    A_NOTER: 2,
    A_CERTIFIER: 3,
    A_PREPARER: 4,
    A_DESCELLER: 7,
    A_VOIR: 6,
    A_DISTRIBUER: 41,
    A_ENVOYER: 42,
    ENVOYEE: 5,
    RECU: 8
  } as const;
  // ========== INTERFACES ==========
  interface Employee {
    id: string
    firstName: string
    lastName: string
    email: string
    status: 'AVAILABLE' | 'BUSY' | 'OFFLINE'
    workHoursPerDay?: number
    workload?: number
    activeOrders?: number
    estimatedHours?: number
    totalCards?: number
    plannings? : planning[]
    assignedOrders? : order[]
    totalWorkload? : number
  }

  interface NewEmployee {
    firstName: string
    lastName: string
    email: string
  }

  // ========== INTERFACES ==========
  interface Planning {
    id: string
    orderId: string
    employeeId: string
    employeeName?: string
    orderNumber?: string
    planningDate: string
    startTime: string
    endTime: string
    durationMinutes: number
    cardCount?: number
    priority?: string
    status?: string
    completed: boolean
    notes?: string
  }

  interface PlanningConfig {
    startDate: string
    cardProcessingTime: number
    priorityMode: string
    redistributeOverload: boolean
    respectPriorities: boolean
  }


}

export {};

import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home.component';
import { AssistantComponent } from './pages/assistant.component';

export const routes: Routes = [
  { path: '', component: HomeComponent, title: 'ProcureSense – Dashboard' },
  { path: 'assistant', component: AssistantComponent, title: 'ProcureSense – Assistant' },
  { path: '**', redirectTo: '' }
];

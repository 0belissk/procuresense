import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AssistantService, AssistantChatResponseDto, ShoppingListItemDto, AssistantChatRequestDto } from '../services/assistant.service';
import { IdentityService } from '../services/identity.service';
import { DemoSettingsService } from '../services/demo-settings.service';

interface ChatMessage {
  role: 'USER' | 'ASSISTANT' | 'SYSTEM';
  text: string;
  timestamp: string;
  variant?: 'default' | 'error' | 'fallback';
}

@Component({
  selector: 'app-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './assistant.component.html',
  styleUrls: ['./assistant.component.scss']
})
export class AssistantComponent {
  readonly scenario = signal('');
  readonly selectedSku = signal('');
  readonly selectedOrgType = signal('distribution');
  readonly selectedProjectType = signal('warehouse setup');
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showFallbackIndicator = signal(false);

  private readonly conversationState = signal<ChatMessage[]>([{
    role: 'ASSISTANT',
    text: 'Tell me about your project or scenario and I will propose a structured shopping kit.',
    timestamp: new Date().toLocaleTimeString(),
    variant: 'default'
  }]);
  readonly conversation = computed(() => this.conversationState());

  private readonly listState = signal<ShoppingListItemDto[]>([]);
  readonly shoppingList = computed(() => this.listState());

  readonly orgId = computed(() => this.identityService.identity().orgId);
  readonly role = computed(() => this.identityService.identity().role);
  readonly useCachedAi = this.demoSettings.useCachedAi;

  constructor(private readonly assistantService: AssistantService,
              private readonly identityService: IdentityService,
              private readonly demoSettings: DemoSettingsService) {}

  sendPrompt(): void {
    if (this.isLoading()) {
      return;
    }
    const trimmed = this.scenario().trim();
    if (!trimmed) {
      this.errorMessage.set('Enter a short scenario to continue.');
      return;
    }
    this.errorMessage.set(null);
    this.appendMessage('USER', trimmed);
    this.isLoading.set(true);
    const payload = this.buildPayload(trimmed);
    this.assistantService.chat(payload).subscribe({
      next: (response: AssistantChatResponseDto) => {
        this.handleAssistantResponse(response);
      },
      error: (err: unknown) => {
        const friendly = this.extractError(err);
        this.appendMessage('SYSTEM', friendly, 'error');
        this.errorMessage.set(friendly);
        this.listState.set([]);
        this.showFallbackIndicator.set(true);
      },
      complete: () => {
        this.isLoading.set(false);
        this.scenario.set('');
      }
    });
  }

  applySamplePrompt(): void {
    this.selectedSku.set('SKU-1002');
    this.selectedOrgType.set('distribution');
    this.selectedProjectType.set('warehouse setup');
    this.scenario.set('We are opening a new satellite warehouse and need a receiving and safety starter kit.');
  }

  clearTranscript(): void {
    this.conversationState.set([{
      role: 'ASSISTANT',
      text: 'Tell me about your project or scenario and I will propose a structured shopping kit.',
      timestamp: new Date().toLocaleTimeString(),
      variant: 'default'
    }]);
    this.listState.set([]);
    this.errorMessage.set(null);
    this.showFallbackIndicator.set(false);
    this.scenario.set('');
    this.selectedSku.set('');
  }

  trackMessage = (_: number, msg: ChatMessage) => `${msg.role}-${msg.timestamp}-${msg.text}`;
  trackItem = (_: number, item: ShoppingListItemDto) => item.sku;

  private handleAssistantResponse(response: AssistantChatResponseDto): void {
    const reply = response.replyText ?? 'Fallback kit ready. These are safe demo items.';
    this.appendMessage('ASSISTANT', reply, this.isFallback(reply) ? 'fallback' : 'default');
    this.listState.set(response.shoppingList ?? []);
    this.showFallbackIndicator.set(this.isFallback(reply));
  }

  private buildPayload(message: string): AssistantChatRequestDto {
    const context: AssistantChatRequestDto['context'] = {};
    if (this.selectedSku().trim()) {
      context.selectedSku = this.selectedSku().trim();
    }
    if (this.selectedOrgType().trim()) {
      context.orgType = this.selectedOrgType().trim();
    }
    if (this.selectedProjectType().trim()) {
      context.projectType = this.selectedProjectType().trim();
    }
    return context && Object.keys(context).length
      ? { message, context }
      : { message };
  }

  private appendMessage(role: ChatMessage['role'], text: string, variant: ChatMessage['variant'] = 'default'): void {
    const message: ChatMessage = {
      role,
      text,
      variant,
      timestamp: new Date().toLocaleTimeString()
    };
    this.conversationState.set([...this.conversationState(), message]);
  }

  private isFallback(text: string): boolean {
    return text.toLowerCase().includes('cached kit') || text.toLowerCase().includes('fallback');
  }

  private extractError(err: unknown): string {
    if (!err) {
      return 'Assistant temporarily unavailable. Using cached kit.';
    }
    if (err instanceof Error) {
      return err.message;
    }
    if (typeof err === 'string') {
      return err;
    }
    return 'Assistant temporarily unavailable. Using cached kit.';
  }
}

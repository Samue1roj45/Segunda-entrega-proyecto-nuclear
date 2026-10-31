import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router, RouterLink } from '@angular/router';

import { Participacion } from '../../core/models/practicas.model';
import { SimulacionService } from '../../core/services/simulacion.service';
import { COMISARIA_SCENE } from './data/comisaria-scene.content';
import {
  COMISARIA_DECISION,
  DecisionOptionDef,
  HOSPITAL_DECISION,
} from './data/decisions.content';
import { DialogueLine, HOSPITAL_SCENE } from './data/hospital-scene.content';
import {
  COMISARIA_ROOM_MAP,
  COMISARIA_ROOMS,
  ComisariaRoomId,
  DEFAULT_COMISARIA_ROOM,
} from './data/comisaria-rooms.content';
import {
  DEFAULT_HOSPITAL_ROOM,
  HOSPITAL_ROOM_MAP,
  HOSPITAL_ROOMS,
  HospitalRoomId,
} from './data/hospital-rooms.content';
import { presentationFor } from './data/character-presentation';
import {
  CARE_ROUTE_STEPS,
  HOSPITAL_TIMELINE_EVENTS,
  RISK_CLASSIFICATION_CARDS,
} from './data/minigames.content';
import { PlayNpcDef, PlayObjectDef, PlaySceneConfig, SceneId } from './data/scene-types';
import { PlaySceneEngine } from './engine/play-scene.engine';
import { InteractableTarget } from './engine/play-types';
import { RouteMinigameComponent } from './minigames/route-minigame.component';
import { RiskMinigameComponent } from './minigames/risk-minigame.component';
import { TimelineMinigameComponent } from './minigames/timeline-minigame.component';
import {
  AcademicMappingAudit,
  DecisionBridgeService,
} from './services/decision-bridge.service';
import { CaseClosureService } from './services/case-closure.service';
import { ConversationService } from './services/conversation.service';
import { ExpedienteCasoService } from './services/expediente-caso.service';
import { InterviewService, InterviewTopicView } from './services/interview.service';
import { ExplorationContextService } from './services/exploration-context.service';
import { SimulacionPlayBridgeService } from './services/simulacion-play-bridge.service';
import { ExpedienteCasoComponent } from './presentation/expediente-caso.component';
import { HypothesisPanelComponent } from './presentation/hypothesis-panel.component';
import { ContenidoSensibleComponent } from './presentation/contenido-sensible.component';
import { IntroNarrativaComponent } from './presentation/intro-narrativa.component';
import { InworldDialogueComponent } from './presentation/inworld-dialogue.component';
import { InterviewPanelComponent } from './presentation/interview-panel.component';
import { DocumentViewerComponent } from './presentation/document-viewer.component';
import { ImmersiveSceneComponent } from './presentation/immersive-scene.component';
import { InvestigationProgressComponent } from './presentation/investigation-progress.component';
import { AmbientAudioService } from './services/ambient-audio.service';
import { InvestigationStatsService } from './services/investigation-stats.service';

interface DialogueState {
  title: string;
  lines: DialogueLine[];
  index: number;
  sourceId: string;
  kind: 'npc' | 'object';
  sceneId: SceneId;
  portraitColor: string;
  tierPlayed: 1 | 2 | 3;
  clueIds: string[];
  mode: 'interview-topic' | 'legacy';
  topicId?: string;
}

interface InterviewPanelState {
  npcId: string;
  npcLabel: string;
  portraitColor: string;
  sceneId: SceneId;
}

interface DocumentViewState {
  title: string;
  lines: DialogueLine[];
  sourceId: string;
  sceneId: SceneId;
  tierPlayed: 1 | 2 | 3;
  clueIds: string[];
  hasMore: boolean;
}

type PlayPhase =
  | 'intro-narrative'
  | 'content-warning'
  | 'explore'
  | 'hypothesis-hospital'
  | 'decision-hospital'
  | 'confirm-hospital'
  | 'consequence-hospital'
  | 'hypothesis-comisaria'
  | 'decision-comisaria'
  | 'confirm-comisaria'
  | 'consequence-comisaria'
  | 'minigame-timeline'
  | 'minigame-route'
  | 'minigame-risk'
  | 'case-synthesis'
  | 'finalize-confirm';

@Component({
  selector: 'app-simulacion-play',
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    TimelineMinigameComponent,
    RouteMinigameComponent,
    RiskMinigameComponent,
    InworldDialogueComponent,
    InterviewPanelComponent,
    DocumentViewerComponent,
    ExpedienteCasoComponent,
    HypothesisPanelComponent,
    IntroNarrativaComponent,
    ContenidoSensibleComponent,
    ImmersiveSceneComponent,
    InvestigationProgressComponent,
  ],
  templateUrl: './simulacion-play.component.html',
  styleUrl: './simulacion-play.component.scss',
})
export class SimulacionPlayComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('playShell') playShellRef?: ElementRef<HTMLElement>;
  @ViewChild('gameCanvas') canvasRef?: ElementRef<HTMLCanvasElement>;

  private readonly simulacion = inject(SimulacionService);
  private readonly bridge = inject(SimulacionPlayBridgeService);
  readonly expediente = inject(ExpedienteCasoService);
  private readonly conversation = inject(ConversationService);
  private readonly interview = inject(InterviewService);
  private readonly closure = inject(CaseClosureService);
  private readonly decisionBridge = inject(DecisionBridgeService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly exploration = inject(ExplorationContextService);
  readonly ambient = inject(AmbientAudioService);
  private readonly invStats = inject(InvestigationStatsService);

  private engine?: PlaySceneEngine;
  private resizeObserver?: ResizeObserver;
  private timerInterval?: ReturnType<typeof setInterval>;
  private npcMap = new Map<string, PlayNpcDef>();
  private objectMap = new Map<string, PlayObjectDef>();

  readonly hospitalDecision = HOSPITAL_DECISION;
  readonly comisariaDecision = COMISARIA_DECISION;
  readonly timelineEvents = HOSPITAL_TIMELINE_EVENTS;
  readonly routeSteps = CARE_ROUTE_STEPS;
  readonly riskCards = RISK_CLASSIFICATION_CARDS;

  readonly loading = signal(false);
  readonly errorInicio = signal<string | null>(null);
  readonly participacion = signal<Participacion | null>(null);
  readonly tiempoRestanteSeg = signal<number | null>(null);
  readonly dialogue = signal<DialogueState | null>(null);
  readonly interviewPanel = signal<InterviewPanelState | null>(null);
  readonly documentView = signal<DocumentViewState | null>(null);
  readonly phase = signal<PlayPhase>('intro-narrative');
  readonly activeSceneId = signal<SceneId>('hospital');
  readonly showTutorialHud = signal(false);
  readonly expedienteOpen = signal(false);
  readonly hospitalRoomId = signal<HospitalRoomId>(DEFAULT_HOSPITAL_ROOM);
  readonly comisariaRoomId = signal<ComisariaRoomId>(DEFAULT_COMISARIA_ROOM);
  readonly explorePaused = signal(false);
  readonly expedienteUnlockPulse = signal(false);
  readonly fullscreenMode = signal(false);
  readonly pendingChoice = signal<DecisionOptionDef | null>(null);
  readonly activeConsequence = signal<DecisionOptionDef | null>(null);
  readonly savingDecision = signal(false);
  readonly finalizing = signal(false);
  readonly mappingAudit = signal<AcademicMappingAudit | null>(null);
  readonly mappingError = signal<string | null>(null);

  readonly clueCount = this.expediente.clueCount;
  readonly investigacionPct = computed(() => {
    if (this.activeSceneId() === 'hospital') return this.expediente.hospitalInvestigationPercent();
    return this.expediente.comisariaInvestigationPercent();
  });

  readonly expedienteSceneLabel = computed(() =>
    this.activeSceneId() === 'hospital' ? 'Urgencias hospitalarias' : 'Comisaría de Familia',
  );

  readonly academicSyncReady = computed(
    () => this.mappingAudit()?.canCompleteWithFullTraceability ?? false,
  );

  readonly sceneConfig = computed<PlaySceneConfig>(() =>
    this.activeSceneId() === 'comisaria' ? COMISARIA_SCENE : HOSPITAL_SCENE,
  );

  readonly sceneTitle = computed(() => this.sceneConfig().title);
  readonly sceneSubtitle = computed(() => this.sceneConfig().subtitle);

  readonly activeDecision = computed(() => {
    const ph = this.phase();
    if (ph.startsWith('decision-hospital') || ph.startsWith('confirm-hospital') || ph === 'consequence-hospital') {
      return this.hospitalDecision;
    }
    if (ph.startsWith('decision-comisaria') || ph.startsWith('confirm-comisaria') || ph === 'consequence-comisaria') {
      return this.comisariaDecision;
    }
    return null;
  });

  readonly showImmersiveExplore = computed(() => this.phase() === 'explore');

  readonly immersiveRoomId = computed(() =>
    this.activeSceneId() === 'comisaria' ? this.comisariaRoomId() : this.hospitalRoomId(),
  );

  readonly immersiveRooms = computed(() =>
    this.activeSceneId() === 'comisaria' ? COMISARIA_ROOMS : HOSPITAL_ROOMS,
  );

  readonly immersiveRoomMap = computed(() =>
    this.activeSceneId() === 'comisaria' ? COMISARIA_ROOM_MAP : HOSPITAL_ROOM_MAP,
  );

  readonly immersiveRoomLabel = computed(() =>
    this.immersiveRoomMap().get(this.immersiveRoomId())?.label ?? 'Institución',
  );

  readonly institutionDisplayTitle = computed(() =>
    this.activeSceneId() === 'hospital' ? 'Hospital San José' : 'Comisaría de Familia',
  );

  readonly professionalNotice = signal<string | null>(null);
  private noticeTimer?: ReturnType<typeof setTimeout>;

  readonly investigationProgress = computed(() =>
    this.invStats.progressFor(this.activeSceneId())(),
  );

  readonly showHud = computed(() => {
    const ph = this.phase();
    return ph !== 'intro-narrative' && ph !== 'content-warning';
  });

  readonly showGameShell = computed(
    () => !this.loading() && !this.errorInicio() && this.showHud(),
  );

  readonly dialoguePortraitColor = computed(() => this.dialogue()?.portraitColor ?? '#0ea5e9');

  readonly dialogueNpcPresentation = computed(() => {
    const d = this.dialogue();
    if (!d || d.kind !== 'npc') return null;
    const npc = this.npcMap.get(d.sourceId);
    return presentationFor(d.sourceId, npc?.label ?? d.title, npc?.role ?? '');
  });

  readonly interviewNpcPresentation = computed(() => {
    const ip = this.interviewPanel();
    if (!ip) return null;
    const npc = this.npcMap.get(ip.npcId);
    return presentationFor(ip.npcId, ip.npcLabel, npc?.role ?? '');
  });

  readonly dialogueLineActual = computed(() => {
    const d = this.dialogue();
    if (!d) return null;
    return d.lines[d.index] ?? null;
  });

  readonly dialogueProgress = computed(() => {
    const d = this.dialogue();
    if (!d || d.lines.length === 0) return '';
    return `${d.index + 1} / ${d.lines.length}`;
  });

  readonly interviewTopics = computed((): InterviewTopicView[] => {
    const panel = this.interviewPanel();
    if (!panel) return [];
    return this.interview.getTopicsForNpc(panel.sceneId, panel.npcId);
  });

  readonly dialogueRegisterLabel = computed(() =>
    this.dialogue()?.mode === 'interview-topic' ? 'Registrar declaración' : 'Registrar en bitácora',
  );

  @HostListener('document:keydown', ['$event'])
  onDocumentKeydown(event: KeyboardEvent): void {
    if (this.phase() !== 'explore') return;
    if (event.key === 'Tab') {
      const tag = (event.target as HTMLElement)?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA') return;
      event.preventDefault();
      this.expedienteOpen.update((v) => !v);
    }
    if (event.key === 'Escape' && this.expedienteOpen()) {
      this.expedienteOpen.set(false);
    }
  }

  ngOnInit(): void {
    this.expediente.reset();
    this.conversation.reset();
    this.interview.reset();
    this.exploration.clear();
  }

  ngAfterViewInit(): void {
    /* Exploración inmersiva por salas; sin canvas. */
  }

  onIntroTerminada(): void {
    this.phase.set('content-warning');
  }

  onSalirSimulacion(): void {
    void this.router.navigate(['/estudiante']);
  }

  onAceptarContenidoSensible(): void {
    this.loading.set(true);
    this.phase.set('explore');
    this.simulacion.iniciarParticipacion().subscribe({
      next: (p) => {
        this.participacion.set(p);
        this.bridge.participacion.set(p);
        const audit = this.decisionBridge.audit(p.caso);
        this.mappingAudit.set(audit);
        if (!audit.canCompleteWithFullTraceability) {
          console.warn('[SimulacionPlay] Auditoría de mapeo académico:', audit);
        }
        this.iniciarTimer(p);
        this.loading.set(false);
        this.entrarHospital();
      },
      error: (err) => {
        this.loading.set(false);
        this.phase.set('content-warning');
        const msg =
          err.error?.detail ||
          err.error?.non_field_errors?.[0] ||
          'No se pudo iniciar la práctica.';
        this.errorInicio.set(msg);
      },
    });
  }

  ngOnDestroy(): void {
    this.engine?.stop();
    this.resizeObserver?.disconnect();
    this.ambient.stop();
    if (this.timerInterval) clearInterval(this.timerInterval);
    if (this.noticeTimer) clearTimeout(this.noticeTimer);
  }

  toggleExpediente(): void {
    this.expedienteOpen.update((v) => !v);
  }

  entrarHospital(): void {
    this.phase.set('explore');
    this.activeSceneId.set('hospital');
    this.hospitalRoomId.set(DEFAULT_HOSPITAL_ROOM);
    this.exploration.setHospitalRoom(DEFAULT_HOSPITAL_ROOM);
    this.rebuildLookup(HOSPITAL_SCENE);
    this.expedienteOpen.set(false);
    this.showTutorialHud.set(true);
    window.setTimeout(() => this.showTutorialHud.set(false), 5000);
    this.engine?.stop();
    void this.ambient.startFor('hospital');
  }

  onImmersiveRoomChange(roomId: string): void {
    if (this.activeSceneId() === 'comisaria') {
      this.comisariaRoomId.set(roomId as ComisariaRoomId);
      this.exploration.setComisariaRoom(roomId as ComisariaRoomId);
    } else {
      this.hospitalRoomId.set(roomId as HospitalRoomId);
      this.exploration.setHospitalRoom(roomId as HospitalRoomId);
    }
  }

  onImmersiveInteract(target: InteractableTarget): void {
    this.onInteract(target);
  }

  toggleAmbient(): void {
    this.ambient.toggle();
    if (this.ambient.enabled() && this.phase() === 'explore') {
      void this.ambient.startFor(this.activeSceneId());
    }
  }

  onObservationRecorded(): void {
    this.notifyExpedienteUnlock();
  }

  async toggleFullscreen(): Promise<void> {
    const el = this.playShellRef?.nativeElement;
    if (!el) return;
    try {
      if (!document.fullscreenElement) {
        await el.requestFullscreen();
      } else {
        await document.exitFullscreen();
      }
    } catch {
      this.fullscreenMode.update((v) => !v);
    }
  }

  @HostListener('document:fullscreenchange')
  onFullscreenChange(): void {
    const el = this.playShellRef?.nativeElement;
    this.fullscreenMode.set(!!el && document.fullscreenElement === el);
  }

  irAComisaria(): void {
    this.phase.set('explore');
    this.activeSceneId.set('comisaria');
    this.comisariaRoomId.set(DEFAULT_COMISARIA_ROOM);
    this.exploration.setComisariaRoom(DEFAULT_COMISARIA_ROOM);
    this.rebuildLookup(COMISARIA_SCENE);
    this.engine?.stop();
    this.setExplorePaused(false);
    void this.ambient.startFor('comisaria');
    this.showProfessionalNotice('Comisaría de Familia — continúa la investigación institucional.');
  }

  onHypothesisSelected(optionId: string): void {
    const ph = this.phase();
    if (ph === 'hypothesis-hospital') {
      this.expediente.setHypothesis('hospital', optionId);
      this.phase.set('decision-hospital');
      this.setExplorePaused(true);
      return;
    }
    if (ph === 'hypothesis-comisaria') {
      this.expediente.setHypothesis('comisaria', optionId);
      this.phase.set('decision-comisaria');
      this.setExplorePaused(true);
    }
  }

  abrirDecisionHospital(): void {
    this.phase.set('decision-hospital');
    this.setExplorePaused(true);
  }

  abrirDecisionComisaria(): void {
    this.phase.set('decision-comisaria');
    this.setExplorePaused(true);
  }

  elegirAccion(option: DecisionOptionDef): void {
    this.mappingError.set(null);
    this.pendingChoice.set(option);
    const ph = this.phase();
    if (ph === 'decision-hospital') this.phase.set('confirm-hospital');
    if (ph === 'decision-comisaria') this.phase.set('confirm-comisaria');
  }

  cancelarConfirmacion(): void {
    this.pendingChoice.set(null);
    const ph = this.phase();
    if (ph === 'confirm-hospital') this.phase.set('decision-hospital');
    if (ph === 'confirm-comisaria') this.phase.set('decision-comisaria');
  }

  confirmarDecision(): void {
    const option = this.pendingChoice();
    const decision = this.activeDecision();
    const part = this.participacion();
    if (!option || !decision || !part) return;

    const ref = this.decisionBridge.resolve(
      part.caso,
      decision,
      option.respuestaOrden,
      option.id,
    );

    if (!ref) {
      this.mappingError.set(
        'No se pudo registrar la intervención. Revisa la configuración del caso.',
      );
      this.snackBar.open(this.mappingError()!, 'OK', { duration: 5000 });
      return;
    }

    this.mappingError.set(null);
    this.savingDecision.set(true);

    this.simulacion.responder(part.id, ref.pregunta_id, ref.respuesta_id).subscribe({
      next: () => {
        const scene = decision.sceneId;
        this.expediente.recordDecision(scene, option.actionTitle);
        this.activeConsequence.set(option);
        this.pendingChoice.set(null);
        this.savingDecision.set(false);

        const ph = this.phase();
        if (ph === 'confirm-hospital') this.phase.set('consequence-hospital');
        if (ph === 'confirm-comisaria') this.phase.set('consequence-comisaria');
      },
      error: () => {
        this.savingDecision.set(false);
        this.mappingError.set('No se pudo registrar la intervención en el expediente.');
        this.snackBar.open(this.mappingError()!, 'OK', { duration: 5000 });
      },
    });
  }

  cerrarConsecuencia(): void {
    const ph = this.phase();
    this.activeConsequence.set(null);
    if (ph === 'consequence-hospital') {
      this.irAComisaria();
      return;
    }
    if (ph === 'consequence-comisaria') {
      this.phase.set('minigame-timeline');
      this.setExplorePaused(true);
    }
  }

  onTimelineDone(): void {
    this.phase.set('minigame-route');
  }

  onRouteDone(): void {
    this.phase.set('minigame-risk');
  }

  onRiskDone(): void {
    this.phase.set('case-synthesis');
  }

  irASintesisFinal(): void {
    this.phase.set('finalize-confirm');
  }

  finalizarParticipacion(): void {
    const part = this.participacion();
    if (!part || this.finalizing()) return;

    this.closure.save(part.id, this.expediente.getSnapshot(part.id));
    this.finalizing.set(true);

    this.simulacion.finalizar(part.id).subscribe({
      next: (res) => {
        this.finalizing.set(false);
        void this.router.navigate(['/estudiante/resultado', res.resultado_id]);
      },
      error: () => {
        this.finalizing.set(false);
        this.snackBar.open('No se pudo cerrar el expediente. Intenta de nuevo.', 'OK', { duration: 5000 });
      },
    });
  }

  private rebuildLookup(scene: PlaySceneConfig): void {
    this.npcMap.clear();
    this.objectMap.clear();
    for (const n of scene.npcs) this.npcMap.set(n.id, n);
    for (const o of scene.objects) this.objectMap.set(o.id, o);
  }

  private scheduleMountEngine(): void {
    window.setTimeout(() => this.mountEngine(), 0);
  }

  private mountEngine(): void {
    if (this.errorInicio() || this.phase() !== 'explore') return;

    const canvas = this.canvasRef?.nativeElement;
    if (!canvas) {
      window.setTimeout(() => this.mountEngine(), 50);
      return;
    }

    if (this.engine) {
      const scene = this.sceneConfig();
      if (this.engine.getSceneId() !== scene.id) {
        void this.engine.prepareAssets().then(() => this.engine?.loadScene(scene));
      } else {
        this.engine.updateSceneEntities(scene);
        this.engine.resize();
      }
      return;
    }

    this.engine = new PlaySceneEngine(canvas, this.sceneConfig(), {
      onInteract: (t) => this.onInteract(t),
    });
    void this.engine
      .prepareAssets()
      .then(() => {
        this.engine?.resize();
        this.engine?.start();
        canvas.focus();
      })
      .catch(() => {
        this.engine?.resize();
        this.engine?.start();
        canvas.focus();
      });

    if (!this.resizeObserver) {
      this.resizeObserver = new ResizeObserver(() => this.engine?.resize());
      this.resizeObserver.observe(canvas.parentElement!);
    }
    canvas.tabIndex = 0;
  }

  onKeyDown(event: KeyboardEvent): void {
    if (this.interviewPanel() && !this.dialogue() && !this.documentView()) {
      if (event.code === 'Escape') {
        event.preventDefault();
        this.cerrarEntrevista();
      }
      return;
    }
    if (this.documentView() && !this.dialogue()) {
      if (event.code === 'Escape') {
        event.preventDefault();
        this.cerrarDocumento();
      }
      return;
    }
    if (this.dialogue()) {
      if (event.code === 'Escape') {
        event.preventDefault();
        this.cerrarDialogo();
        return;
      }
      if (event.code === 'KeyE' || event.code === 'Enter') {
        event.preventDefault();
        this.avanzarDialogo();
      }
      return;
    }
    if (this.phase() !== 'explore') return;
    if (this.engine?.handleKeyDown(event.code)) {
      event.preventDefault();
      return;
    }
    event.preventDefault();
  }

  onKeyUp(event: KeyboardEvent): void {
    if (this.phase() === 'explore') {
      this.engine?.handleKeyUp(event.code);
    }
  }

  onCanvasClick(event: MouseEvent): void {
    if (this.dialogue()) {
      this.avanzarDialogo();
      return;
    }
    if (this.phase() === 'explore') {
      this.engine?.handlePointer(event.clientX, event.clientY);
    }
  }

  onInteract(target: InteractableTarget): void {
    if (this.dialogue() || this.interviewPanel() || this.documentView()) return;
    const sceneId = this.activeSceneId();

    if (target.kind === 'npc') {
      const npc = this.npcMap.get(target.id);
      if (!npc) return;
      if (!this.interview.hasAvailableTopics(sceneId, npc.id)) {
        this.snackBar.open('Por ahora no hay más líneas de entrevista disponibles.', 'OK', { duration: 2500 });
        return;
      }
      const pres = presentationFor(npc.id, npc.label, npc.role);
      this.interviewPanel.set({
        npcId: npc.id,
        npcLabel: pres.nombreCompleto,
        portraitColor: npc.color,
        sceneId,
      });
      this.setExplorePaused(true);
      return;
    }

    const obj = this.objectMap.get(target.id);
    if (!obj) return;
    const conv = this.conversation.startConversation(sceneId, 'object', obj.id);
    if (!conv) {
      this.snackBar.open('Ya revisaste este material en profundidad.', 'OK', { duration: 2500 });
      return;
    }
    this.documentView.set({
      title: obj.inspectTitle,
      lines: conv.lines,
      sourceId: obj.id,
      sceneId,
      tierPlayed: conv.tierPlayed,
      clueIds: conv.clueIds,
      hasMore: conv.hasMore,
    });
    this.setExplorePaused(true);
  }

  onSeleccionarTemaEntrevista(topicId: string): void {
    const panel = this.interviewPanel();
    if (!panel) return;
    const topic = this.interview.startTopic(topicId);
    if (!topic) {
      this.snackBar.open('Este tema no está disponible aún.', 'OK', { duration: 2200 });
      return;
    }
    this.abrirDialogo({
      title: panel.npcLabel,
      lines: topic.lines,
      index: 0,
      sourceId: panel.npcId,
      kind: 'npc',
      sceneId: panel.sceneId,
      portraitColor: panel.portraitColor,
      tierPlayed: topic.tier,
      clueIds: topic.clueIds,
      mode: 'interview-topic',
      topicId: topic.id,
    });
  }

  cerrarEntrevista(): void {
    this.interviewPanel.set(null);
    this.dialogue.set(null);
    this.setExplorePaused(false);
    this.canvasRef?.nativeElement?.focus();
  }

  registrarDocumento(): void {
    const doc = this.documentView();
    if (!doc) return;
    this.conversation.completeConversation(
      doc.sceneId,
      'object',
      doc.sourceId,
      doc.tierPlayed,
      doc.clueIds,
    );
    this.notifyExpedienteUnlock();
    this.documentView.set(null);
    this.setExplorePaused(false);
    this.canvasRef?.nativeElement?.focus();
    this.checkInvestigationThreshold();
  }

  cerrarDocumento(): void {
    this.documentView.set(null);
    this.setExplorePaused(false);
    this.canvasRef?.nativeElement?.focus();
  }

  abrirDialogo(state: DialogueState): void {
    this.dialogue.set(state);
    this.setExplorePaused(true);
  }

  avanzarDialogo(): void {
    const d = this.dialogue();
    if (!d) return;
    if (d.index < d.lines.length - 1) {
      this.dialogue.set({ ...d, index: d.index + 1 });
    } else {
      this.cerrarDialogoYRegistrar(d);
    }
  }

  cerrarDialogo(): void {
    const d = this.dialogue();
    if (d?.mode === 'interview-topic') {
      this.limpiarDialogo();
      return;
    }
    if (d) this.cerrarDialogoYRegistrar(d);
    else this.limpiarDialogo();
  }

  private cerrarDialogoYRegistrar(d: DialogueState): void {
    if (d.mode === 'interview-topic' && d.topicId) {
      const topic = this.interview.getTopic(d.topicId);
      if (topic) {
        this.interview.completeTopic(topic);
      }
      this.notifyExpedienteUnlock();
      this.dialogue.set(null);
      this.setExplorePaused(!!this.interviewPanel());
      if (!this.interviewPanel()) {
        this.canvasRef?.nativeElement?.focus();
      }
    } else {
      this.conversation.completeConversation(
        d.sceneId,
        d.kind,
        d.sourceId,
        d.tierPlayed,
        d.clueIds,
      );
      this.notifyExpedienteUnlock();
      this.limpiarDialogo();
    }
    this.checkInvestigationThreshold();
  }

  private limpiarDialogo(): void {
    this.dialogue.set(null);
    const enEntrevista = !!this.interviewPanel();
    this.setExplorePaused(enEntrevista);
    if (!enEntrevista) {
      this.canvasRef?.nativeElement?.focus();
    }
  }

  private setExplorePaused(paused: boolean): void {
    this.explorePaused.set(paused);
    this.engine?.setPaused(paused);
  }

  private notifyExpedienteUnlock(): void {
    this.expedienteUnlockPulse.set(true);
    window.setTimeout(() => this.expedienteUnlockPulse.set(false), 2800);
    const last = this.expediente.ultimoRegistro();
    if (last) {
      const fuente =
        last.fuente === 'entrevista'
          ? 'Entrevista'
          : last.fuente === 'documento'
            ? 'Documento'
            : 'Observación';
      this.showProfessionalNotice(
        `${fuente} · ${last.ubicacion} · ${last.informante}`,
      );
    }
  }

  private showProfessionalNotice(text: string): void {
    this.professionalNotice.set(text);
    if (this.noticeTimer) clearTimeout(this.noticeTimer);
    this.noticeTimer = setTimeout(() => this.professionalNotice.set(null), 4000);
  }

  private checkInvestigationThreshold(): void {
    if (this.phase() !== 'explore') return;

    if (this.activeSceneId() === 'hospital' && this.expediente.hospitalReady()) {
      this.phase.set('hypothesis-hospital');
      this.setExplorePaused(true);
      this.showProfessionalNotice(
        'Evidencia hospitalaria suficiente. Formula tu hipótesis profesional.',
      );
      return;
    }

    if (this.activeSceneId() === 'comisaria' && this.expediente.comisariaReady()) {
      this.phase.set('hypothesis-comisaria');
      this.setExplorePaused(true);
      this.showProfessionalNotice(
        'Investigación institucional completa. Reevalúa tu hipótesis con las nuevas fuentes.',
      );
    }
  }

  tiempoFormato(): string {
    const seg = this.tiempoRestanteSeg();
    if (seg === null) return '--:--';
    const m = Math.floor(seg / 60);
    const s = seg % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  private iniciarTimer(p: Participacion): void {
    const practicaActiva = JSON.parse(localStorage.getItem('simulador.practica_activa') || '{}');
    const tiempoMaxMin: number = practicaActiva.tiempo_max_min || 30;
    const inicio = p.inicio ? new Date(p.inicio).getTime() : Date.now();
    const finEsperado = inicio + tiempoMaxMin * 60 * 1000;

    const tick = () => {
      const restante = Math.max(0, Math.floor((finEsperado - Date.now()) / 1000));
      this.tiempoRestanteSeg.set(restante);
    };
    tick();
    this.timerInterval = setInterval(tick, 1000);
  }
}
